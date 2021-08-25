package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ExampleDynamoDbHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build();

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        context.getLogger().log(input.toString());

        try {
            Map<String, AttributeValue> itemAttributes = new HashMap<>();
            itemAttributes.put("id", AttributeValue.builder().s(context.getAwsRequestId()).build());
            itemAttributes.put("value",
                    AttributeValue.builder().s(
                            input.getHeaders().entrySet()
                                    .stream()
                                    .map(a -> a.getKey()+"="+a.getValue())
                                    .collect(Collectors.joining())
                    ).build()
            );

            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(itemAttributes)
                    .build());
            return response.withBody("successful").withStatusCode(200);
        } catch (DynamoDbException e) {
            context.getLogger().log(e.getMessage());
            return response.withBody("error").withStatusCode(500);
        }
    }

}
