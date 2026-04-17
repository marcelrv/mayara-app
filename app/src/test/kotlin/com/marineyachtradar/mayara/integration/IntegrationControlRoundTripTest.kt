package com.marineyachtradar.mayara.integration

import app.cash.turbine.test
import com.marineyachtradar.mayara.data.api.RadarApiClient
import com.marineyachtradar.mayara.data.api.SignalKStreamClient
import com.marineyachtradar.mayara.data.api.SpokeWebSocketClient
import com.marineyachtradar.mayara.data.model.RadarUiState
import com.marineyachtradar.mayara.domain.RadarRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * Integration test for the radar control write (PUT) round-trip.
 *
 * Verifies that:
 *   1. [RadarRepository.setSliderControl] issues a correctly formatted HTTP PUT request.
 *   2. The PUT request body contains {"value": <n>}.
 *   3. The server receives the request on the expected URL path.
 *   4. The control is updated locally (optimistic update) so the UI reflects the change.
 */
class IntegrationControlRoundTripTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RadarRepository
    private lateinit var testScope: TestScope

    private val radarId = "radar0"

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        testScope = TestScope(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        if (::repository.isInitialized) repository.disconnect()
        testScope.cancel()
        try { server.shutdown() } catch (_: Exception) { /* OkHttp WS close race */ }
    }

    private fun baseUrl() = server.url("/").toString().trimEnd('/')

    private fun buildRepository(): RadarRepository {
        return RadarRepository(
            apiClient = RadarApiClient(baseUrl()),
            spokeClient = SpokeWebSocketClient(),
            streamClient = SignalKStreamClient(),
            scope = testScope,
        )
    }

    private fun enqueueConnectFlow() {
        val base = baseUrl()
        val spokeWsUrl = "${base.replace("http", "ws")}/signalk/v2/api/vessels/self/radars/$radarId/spokes"
        server.enqueue(
            MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody("""{"$radarId":{"name":"Test Radar","brand":"Emulator","spokeDataUrl":"$spokeWsUrl","streamUrl":"${base.replace("http","ws")}/signalk/v1/stream"}}""")
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody("""{"supportedRanges":[600,1200,3000,6000],"spokesPerRevolution":4096,"maxSpokeLength":512,"controls":{"gain":{"id":"gain","name":"Gain","dataType":"number","minValue":0,"maxValue":100}}}""")
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody("""{"gain":{"value":50.0}}""")
        )
        server.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onOpen(ws: okhttp3.WebSocket, response: okhttp3.Response) {}
        }))
        server.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onOpen(ws: okhttp3.WebSocket, response: okhttp3.Response) {}
        }))
    }

    /** Drains all recorded requests from MockWebServer and returns them. */
    private fun drainRequests(extraWaitSeconds: Long = 2): List<RecordedRequest> {
        val requests = mutableListOf<RecordedRequest>()
        // Drain any already-queued requests quickly
        var req = server.takeRequest(100, TimeUnit.MILLISECONDS)
        while (req != null) {
            requests.add(req)
            req = server.takeRequest(100, TimeUnit.MILLISECONDS)
        }
        // One longer wait for any in-flight requests (e.g. PUT on IO thread)
        req = server.takeRequest(extraWaitSeconds, TimeUnit.SECONDS)
        if (req != null) {
            requests.add(req)
            // Drain any remaining
            req = server.takeRequest(100, TimeUnit.MILLISECONDS)
            while (req != null) {
                requests.add(req)
                req = server.takeRequest(100, TimeUnit.MILLISECONDS)
            }
        }
        return requests
    }

    @Test
    fun `setSliderControl sends correct PUT request path`() = runTest {
        enqueueConnectFlow()
        server.enqueue(MockResponse().setResponseCode(200))
        repository = buildRepository()

        repository.uiState.test {
            awaitItem() // Loading
            repository.connect(baseUrl())
            awaitItem() // Connected
            repository.setSliderControl("gain", 75f, false)
            awaitItem() // optimistic update
            cancelAndIgnoreRemainingEvents()
        }

        val putRequest = drainRequests().firstOrNull { it.method == "PUT" }
        assertNotNull(putRequest, "Expected a PUT request to be sent")
        assertEquals(
            "/signalk/v2/api/vessels/self/radars/$radarId/controls/gain",
            putRequest!!.path
        )
    }

    @Test
    fun `setSliderControl PUT body contains correct value`() = runTest {
        enqueueConnectFlow()
        server.enqueue(MockResponse().setResponseCode(200))
        repository = buildRepository()

        repository.uiState.test {
            awaitItem() // Loading
            repository.connect(baseUrl())
            awaitItem() // Connected
            repository.setSliderControl("gain", 60f, false)
            awaitItem() // optimistic update
            cancelAndIgnoreRemainingEvents()
        }

        val putRequest = drainRequests().firstOrNull { it.method == "PUT" }
        assertNotNull(putRequest, "Expected a PUT request")
        val body = putRequest!!.body.readUtf8()
        assertTrue(body.contains("60")) { "Expected body to contain '60', got: $body" }
    }

    @Test
    fun `setSliderControl applies optimistic update to controls state`() = runTest {
        enqueueConnectFlow()
        server.enqueue(MockResponse().setResponseCode(200))
        repository = buildRepository()

        repository.uiState.test {
            awaitItem() // Loading
            repository.connect(baseUrl())
            val connected = awaitItem() as RadarUiState.Connected
            assertEquals(50f, connected.controls.gain.value, 0.01f)

            repository.setSliderControl("gain", 80f, false)

            val updated = awaitItem() as RadarUiState.Connected
            assertEquals(80f, updated.controls.gain.value, 0.01f,
                "Optimistic update should reflect gain=80 immediately")

            cancelAndIgnoreRemainingEvents()
        }
    }
}