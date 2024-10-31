import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

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
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9093");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "your-group-id");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        try (Connection connection = DriverManager.getConnection(dbUrl, user, password)) {
            String insertSQL = "INSERT INTO events (time, event_type, event_data) VALUES (?, ?, ?)";

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
                        pstmt.setTimestamp(1, Timestamp.from(record.timestamp()));
                        pstmt.setString(2, record.key());
                        pstmt.setString(3, record.value());
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
