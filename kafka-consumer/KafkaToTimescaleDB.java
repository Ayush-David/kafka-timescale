import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaToTimescaleDB {
    public static void main(String[] args) {
        String topic = "quickstart";

        // Load environment variables
        String dbUrl = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        // Configure Kafka Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "your-group-id");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        try (Connection connection = DriverManager.getConnection(dbUrl, user, password)) {
            String insertSQL = "INSERT INTO events (time, occupancy, entry, exit, source_id) VALUES (?, ?, ?, ?, ?)";
            ObjectMapper objectMapper = new ObjectMapper();

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
                        JsonNode jsonNode = objectMapper.readTree(record.value());

                        pstmt.setTimestamp(1, Timestamp.valueOf(jsonNode.get("timestamp").asText()));
                        pstmt.setInt(2, jsonNode.get("occupancy").asInt());
                        pstmt.setInt(3, jsonNode.get("Entry").asInt());
                        pstmt.setInt(4, jsonNode.get("Exit").asInt());
                        pstmt.setInt(5, jsonNode.get("source_id").asInt());
                        pstmt.executeUpdate();
                    } catch (Exception e) {
                        System.err.println("Error inserting data: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
