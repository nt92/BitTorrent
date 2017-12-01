import java.util.concurrent.TimeUnit;

import static java.lang.System.exit;

public class SampleEnvironment {
    public static void main(String[] args) {
        for (int i=1001; i < 1007; i++){
            int intVal = i;
            new Thread(() -> {
                try {
                    peerProcess.main(new String[] {"" + intVal});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Delaying start time of the peers by a second so that they begin in the correct order
            try{
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch(Exception e){
                System.out.println(e);
                exit(1);
            }
        }
    }
}
