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
            System.out.println("Read: "+ request.pathInfo());
            r.read(request.pathInfo());
            return request.pathInfo();
        });

        post("/files/*", (request, response) -> {
            System.out.println("Write: "+ request.pathInfo());
            r.write(request.pathInfo());
            return request.pathInfo();
        });

        put("/files/*", (request, response) -> {
            System.out.println("Modify metadata: "+ request.pathInfo());

            return request.pathInfo();
        });
    }
}
