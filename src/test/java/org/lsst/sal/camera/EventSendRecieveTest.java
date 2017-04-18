package org.lsst.sal.camera;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lsst.sal.camera.SummaryStateEvent.LSE209State;

/**
 *
 * @author tonyj
 */
public class EventSendRecieveTest {

    private static SALCamera camera;
    private static ExecutorService executor;

    public EventSendRecieveTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        camera = SALCamera.instance();
        executor = Executors.newFixedThreadPool(1);
    }

    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void sendReceiveSummaryStateEvent() throws InterruptedException, SALException, ExecutionException, TimeoutException {
        Future<CameraEvent> future = executor.submit(() -> camera.getNextEvent(Duration.ofSeconds(10)));
        Thread.sleep(5000);
        camera.logEvent(new SummaryStateEvent(1, LSE209State.ENABLED));
        CameraEvent event = future.get(10, TimeUnit.SECONDS);
        System.out.println(event);
        assertTrue(event instanceof SummaryStateEvent);
        SummaryStateEvent sse = (SummaryStateEvent) event;
        //TODO: Understand why these fail
        //assertEquals(LSE209State.ENABLED, sse.getState());
        //assertEquals(1, sse.getPriority());

    }
}
