package services;

import static spark.Spark.*;

public class RequestService {
    private RedisService r;

    public RequestService(RedisService r) {
        this.r = r;
        setupEndpoints();
    }

    private void setupEndpoints() {
        port(8080);

        get("/admin/flush", (request, response) -> {
            r.flush();
            System.out.println("Flushed redis");
            return "Flushed redis";
        });

        get("/admin/filelocations", (request, response) -> r.getFileLocations());

        get("/files/*", (request, response) -> {
            String fileName = request.pathInfo().substring(7);
            System.out.println("Read: " + fileName);
            return ("Read file " + fileName + " which had value of " + r.read(fileName));
        });

        post("/files/*", (request, response) -> {
            String fileName = request.pathInfo().substring(7);
            System.out.println("Write: " + fileName);
            r.write(fileName, "test");
            return ("Writing file " + fileName);
        });

        put("/files/*", (request, response) -> {
            System.out.println("Modify metadata: " + request.pathInfo());

            return request.pathInfo();
        });
    }
}
