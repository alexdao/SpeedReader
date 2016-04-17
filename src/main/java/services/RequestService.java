package services;

import static spark.Spark.*;


/**
 * Created by jiaweizhang on 4/12/16.
 */
public class RequestService {
    private RedisService r;

    public RequestService(RedisService r) {
        this.r = r;
        setupEndpoints();
    }

    private void setupEndpoints() {
        port(8080);

        get("/files/*", (request, response) -> {
            String fileName = request.pathInfo().substring(7);
            System.out.println("Read: "+ fileName);
            StringBuilder sb = new StringBuilder();
            sb.append("Reading file '"+fileName);
            sb.append("' from server number " + r.read(fileName));
            return sb.toString();
        });

        post("/files/*", (request, response) -> {
            String fileName = request.pathInfo().substring(7);
            System.out.println("Write: "+ fileName);
            StringBuilder sb = new StringBuilder();
            sb.append("Writing file '"+fileName);
            sb.append("' to server number " + r.write(fileName));
            return sb.toString();
        });

        put("/files/*", (request, response) -> {
            System.out.println("Modify metadata: "+ request.pathInfo());

            return request.pathInfo();
        });
    }
}
