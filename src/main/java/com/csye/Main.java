package com.csye;

import com.google.cloud.functions.CloudEventsFunction;
import com.google.events.cloud.pubsub.v1.MessagePublishedData;
import com.google.gson.Gson;
import io.cloudevents.CloudEvent;
import net.sargue.mailgun.Configuration;
import net.sargue.mailgun.Mail;
import net.sargue.mailgun.Response;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.logging.Logger;

public class Main implements CloudEventsFunction {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final String API_KEY = System.getenv("MAILGUN_API_KEY");
    private static final String DOMAIN = System.getenv("DOMAIN");
    private final String host = System.getenv("DB_HOST");
    private final String user = System.getenv("DB_USERNAME");
    private final String password = System.getenv("DB_PASSWORD");
    private final String database = System.getenv("DB_NAME");
    private final String port = System.getenv("DB_PORT");

    @Override
    public void accept(CloudEvent event) {
        try {
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
            logger.info("Connecting to database: " + url);
            Connection conn = DriverManager.getConnection(url, user, password);
            logger.info("Connection established");
            if (event.getData() != null) {
                String cloudEventData = new String(event.getData().toBytes(), StandardCharsets.UTF_8);
                Gson gson = new Gson();
                MessagePublishedData data = gson.fromJson(cloudEventData, MessagePublishedData.class);
                String email = data.getMessage().getData();
                String decodedData =
                        new String(Base64.getDecoder().decode(email), StandardCharsets.UTF_8).replaceAll("\n", "");
                String sql = "select id from person where username = " + "'" + decodedData + "'";
                System.out.println("SQL: " + sql);
                PreparedStatement preparedStatement = conn.prepareStatement(sql);

                ResultSet rs = preparedStatement.executeQuery();
                String id;
                if (rs.next()) {
                    id = rs.getString("id");
                    logger.info("User found: " + id);

                    Configuration configuration = new Configuration()
                            .domain(DOMAIN)
                            .apiKey(API_KEY)
                            .from("Joseph Alex Chakola", "josephalex@" + DOMAIN);

                    Response response = Mail.using(configuration)
                            .to(decodedData.trim())
                            .subject("Verify your email address")
                            .text("Please click the link to verify your email: https://" + DOMAIN + ":443/user/verify?token=" + id)
                            .build()
                            .send();

                    if (response.isOk()) {
                        logger.info("Mail sent successfully.");
                        // Update the user table with the verification time
                        String updateSql = "update person set verification_time = now() where id = " + "'" + id + "'";
                        PreparedStatement updateStatement = conn.prepareStatement(updateSql);
                        updateStatement.executeUpdate();
                    } else {
                        logger.warning("Error sending mail: " + response.responseMessage());
                    }
                } else {
                    logger.warning("User not found: " + decodedData);
                }
                conn.close();
                logger.info("Connection closed");
            }
        } catch (Exception e) {
            logger.warning("Error: " + e.getMessage());
        }

    }
}
