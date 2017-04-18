package org.lsst.sal.camera;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author tonyj
 */
public class CommandSendReceiveTest {

    private static SALCamera camera;
    private static ExecutorService executor;

    public CommandSendReceiveTest() {
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
    public void sendReceiveEnableCommand() throws InterruptedException, SALException, ExecutionException, TimeoutException {
        Future<CameraCommand> future = executor.submit(() -> camera.getNextCommand(Duration.ofSeconds(10)));
        Thread.sleep(5000);
        CommandResponse response = camera.issueCommand(new EnableCommand());
        CameraCommand command = future.get(10, TimeUnit.SECONDS);
        command.reportComplete();
        System.out.println(command);
        assertTrue(command instanceof EnableCommand);
        response.waitForResponse(Duration.ofSeconds(1));
    }
}
