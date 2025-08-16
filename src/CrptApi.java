import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private final HttpClient httpClient;
    private final Lock lock;
    private final long delayBetweenRequests;
    private long lastRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestsPerTimeUnit) {
        if (requestsPerTimeUnit <= 0) {
            throw new IllegalArgumentException("Лимит запросов должен быть положительным");
        }

        this.httpClient = HttpClient.newHttpClient();
        this.lock = new ReentrantLock();
        this.delayBetweenRequests = timeUnit.toMillis(1) / requestsPerTimeUnit;
        this.lastRequestTime = System.currentTimeMillis() - delayBetweenRequests;
    }

    public void createDocument(Document document, String signature) {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRequest = currentTime - lastRequestTime;

            if (timeSinceLastRequest < delayBetweenRequests) {
                try {
                    Thread.sleep(delayBetweenRequests - timeSinceLastRequest);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Запрос был прерван", e);
                }
            }

            lastRequestTime = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }

        try {
            String jsonDocument = convertToJson(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Ошибка API: код " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при отправке документа", e);
        }
    }

    private String convertToJson(Document document) {

        return "{}";
    }

    public static class Document {
        private String participantInn;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private Product[] products;


        public String getParticipantInn() { return participantInn; }
        public void setParticipantInn(String participantInn) { this.participantInn = participantInn; }

    }


    public static class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;

        public String getCertificateDocument() { return certificateDocument; }
        public void setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
        }
    }
}