import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class TestDistributedApplication {

    private static void reset() {
        HttpResponse<String> result = null;
        try {
            result = Unirest.get("http://localhost:8080/admin/flush")
                    .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        String str = result.getBody();
        System.out.println(str);
    }

    private static Client[] makeClients(int n) {
        Client[] res = new Client[n];
        for (int i = 0; i < n; i++) {
            res[i] = new Client(i + 1);
        }
        return res;
    }

    private static void Test1() {
        reset();
        Client[] clients = makeClients(2);
        clients[0].write("file1", "2");
        clients[0].read("file1");
        clients[1].write("file2", "4");
        clients[1].read("file1");
        clients[0].read("file2");
        clients[0].write("file2", "5", 0);
        clients[0].read("file2");
        clients[0].read("file2");
        clients[0].read("file2");
        clients[0].read("file2");
        clients[0].read("file2");
        clients[0].read("file2");
        clients[0].read("file2");
        clients[0].read("file2");
        clients[0].read("file2");
        clients[0].read("file2");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        clients[0].write("file2", "10", 2);
        clients[0].read("file2");
        getFileLocations();
    }

    public static void main(String[] args) {
        Test1();
    }


    private static void getFileLocations() {
        HttpResponse<String> result = null;
        try {
            result = Unirest.get("http://localhost:8080/admin/filelocations")
                    .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        String str = result.getBody();
        System.out.println();
        System.out.println(str);
        System.out.println();
    }
}
