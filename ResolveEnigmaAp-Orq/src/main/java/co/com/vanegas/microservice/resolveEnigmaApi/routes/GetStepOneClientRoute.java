package co.com.vanegas.microservice.resolveEnigmaApi.routes;

import co.com.vanegas.microservice.resolveEnigmaApi.client.ClientJsonApiBodyResponseSuccess;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

@Component
public class GetStepOneClientRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:get-step-one")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("freemarker:templates/SetStepOneClientTemplate.ftl")
                    .log("Request microservice step one ${body}")
                .hystrix()
                .hystrixConfiguration().executionTimeoutInMilliseconds(2000).end()
                .to("http://localhost:8080/microservice/step/${step}")
                    .convertBodyTo(String.class)
                    .unmarshal().json(JsonLibrary.Jackson, ClientJsonApiBodyResponseSuccess.class)
                    .log("Response microservice step one ${body}")
                .process(new Processor() {

                    public void process(Exchange exchange) throws Exception {
                        ClientJsonApiBodyResponseSuccess stepOneResponse = (ClientJsonApiBodyResponseSuccess) exchange.getIn().getBody();
                        if (stepOneResponse.getData().get(0).getStepId().equalsIgnoreCase("1")) {
                            exchange.setProperty("step", "Step1");
                            stepOneResponse.getData().get(0).getStepDescription();
                            exchange.setProperty("error", "000");
                            exchange.setProperty("descError", "No error");
                        } else {
                            exchange.setProperty("error", "0001");
                            exchange.setProperty("descError", "Error consulting the step one");
                        }
                    }
                })
                .endHystrix()
                .onFallback()
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.setProperty("Error","0002");
                        exchange.setProperty("descError","Error consulting the step one");
                    }
                })
                .end()
                .log("Response description ${exchangeProperty[descError]")
                .log("Response code ${exchangeProperty[Error]}");
    }
}
