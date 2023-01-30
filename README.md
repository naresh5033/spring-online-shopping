### spring-boot-microservices

As per the Solutioin Arch, the implementation Gon be Product service, inventory service, Order service and the Notification service

## Asynchronous communication

For the Async communication amongst these service i used Kafka, Rabbitmq

## Synchronous communication

And for the sync communicatioin Reselience4j will be used

## API gateway

The API gateway of our cluster will be secured by authorized server called as Keycloack

## Product service

For the product service we use the mongo as db

## Order service

for the order service we use the mssql as db, but in prod i will swith to postgres

## inventory service

for the Inventory service we use the mssql as db

`mvn clean verify` --> (to exec the maven goal) will build the proj, w/o tryna install everything

## Synchronous communication

For the iner process communication b/w order serv and the inventory serv we'll use the webclient from spring frameworks web reactive client..

- so need to create a config package inside the order service

## Discovery server

The service discovery is a pattern where the discovery server will register all the ip addr of the services and the services can refer to this server for the communication amongst them.

- so whenever the services are making the req the discovery server will add the services to its registry.
- when the client making the initial call to discovery server, it will send its local copy to the client, so if discovery service isn't available, the client can refer its local copy.
- We can use the spring cloud's NETFLIX EUREKA module as the discovery server.
- And for the each services we ve to add the eureka client in the dependency and enble the eureka client on all the services.
- Inside the inv service - set the port numb to 0 so the spring will pick rand port and then edit the config and apply allow parallel run in IDE --> which will allow us to run multiple instances of the app.

# client side LB

When constructing the webclient we ve to annotate the client side LB, this will add the LB capability to the webclient builder, this will use the web client LB to the inventory service, so by this way even if we ve multiple instance of the inv serv, the order serv will not panic it will call the inv serv one after another based on the discovery server.\

## API Gateway

- Routing based req header
- Authentication
- security
- LB
- SSL Termination

We'll use the spring cloud gateway as our api gateway

## KeyCloak

As per our solution arch we re gon to use keycloak as the auth security to our api gateway.

- The keycloak is the authentication and authorization server.
- To start the keycloak server ```docker run -p 8181:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.3 start-dev```
- This will start Keycloak exposed on the local port 8080. It will also create an initial admin user with username admin and password admin.
- Then in the admin console we can create a realm/ the services we wanna add to the Oauth, oAuth client.
- the client id (spring-cloud-client), and the access type - confidential, disable the (std flow enabled and direct acess grant) enable the (service acc)- this will enable the client credentials grant.
- this will create the client secret just copy the secret Uid(later we ll use in the postman to get the oAuth2 jwt)
- Then in the realm settings -> endpt -> openId endpt config and copy the issuer uri obj and then paste it in the api gateway application property.
- for the prod we'll use mysql db for keycloak
- Note : In the realm dir we ve complete metadata abt the keycloak -> realms.json

Now if the client wana access the service they ve to provide the bearer token a JWT token, so we ve to create/req the token from keycloak by providing our credentials.

## Circuit Breaker 

The circuit Breaker is set of states that we maintain in our app, 
- if the By default its in closed state and the communication b/w our app is proper
- if any service is break down or not working the circuit breaker will be in the open state.
- And in the open state it won't allow any calls b/w the services, and we can exec some fallback logic.

So to write the logic we gon use the service called Resilience4J

# Resilience4j

Is a light weight easy to use FT library mainly used by Netflix hystrix

Once we impl the logic we can see the actuator health(state of circuit breaker) in http://localhost:8081/actuator/health --> we can also see all the events that are triggered by the resilience4j, such as retry event, timeout event etc

## Distributed Tracing

It helps us to trace the event from start to finish, so if the request is failed at any point of time we can know how it failed and where it failed.
- we can use TraceId to trace the requests those are into our s/m
- spanID is the no.of trips inside our s/m, we ve one trip to api GW, one trip to order service, one trip to inv service.
- and for all these trips we ve unique Identifier
By using these traceId and spanId we can trace the whole request life cycle in our services.

For this we can use the "spring cloud sleuth" is a distributed tracing framework, which helps us to generate the traceId and spanId whenever we receive a request to the microservices.

- And to visualize this we can use "zipkin" --> ```docker run -d -p 9411:9411 openzipkin/zipkin```
runs at http://localhost:9411/zipkin --> run the query and see all the reqs that made 
This sleuth and zipkin we ve to add in the property for each service.
Now if we send a req amongst our services we can see the spanId and TraceId.
- The spring cloud sleuth provides us the mechanism to add our own spanId, We can do that by using the tracer class by spring cloud sleuth.

## Event driven Arch (Apache Kafka)

The Async communication can be achieved by using this event driven arch in the form of events 
 - in our scenerio the order service will place the order placed event (which will be the producer), and the notification service(will be the consumer, consume the msg and process accordingly) which will sub the topic of the event.\
 - "Apache Kafka" will be the our msg broker --> we will use the docker compose file to deploy the kafka.\
 - The docker compose file will ve couple of services 1. zookeper 2. broker
 - ```docker compose up -d```  
 - ```docker ps```
 - ```docker logs -f broker```
# spring for Kafka

The spring for apache kafka (spring-kafka) project applies core spring concepts to the development of kafka based messaging solns.

## Dockerize 

Now lets dockerize our all apps ```doker build -t apigateway-dockerfile .```

Note : This docker build img will not be having any contextual understanding of what to build or what not to, its just build the whole img.
- For this reason we ve to use a mechanism called "Multi Stage Builds" this will improve process of our docker build file
- "Dockerfile.layered" --> basically in this file we'll just copy diff layers from the target folder and put em all together in the extracted folder and exposed port 8080 and run the cmd java load jarlauncher
- now that we ve that docker layered file ```docker build -t apigateway-layered -f Dockerfile.layered``` --> now this will build our docker img into diff layers.
- ```docker images``` --> in this we can see the imgs the normal img is bigger in size as compared with the layered img.

# JIB 

JIB is a plugin that builds docker container w/o requiring docker installation, JIB handles all steps of packaging our app into container img.
- after add this plugin our root pom, let run in maven --> plugins-> jib -> and the goal is ``jib build`` this will create and push the docker img into hub. --> ```maven clean compile jib:build``` --> before running this make sure we ve our docker hub credentials added in the settings.xml file.
- This will add all our imgs into the hub, and finally configured all the serv and DB into a single docker compose file in our root proj repo
- now we can run docker compose file -> ```docker compose up -d ```  --> will pull all our containers and run em up in daemon mode.
- to see the logs of particular container ```docker logs -f broker```

Finally Redirects all the traffic to the localhost to the keycloak by adding "127.0.0.1 keycloak" in the host file.

## Monitoring 

For the monitoring we will be using Prometheus and Grafana.

# Prometheus 

The prometheus will grab all our spring boot metrics(polls every X secs for metrics) from our apps and it will store it in the inMem DB and then we will visualize those metrics by using grafana.
- Add the dependency in each serv and then config the actuator endpoint in the prop file (in all the services)--> so we can access as /actuator/prometheus/ in our srv
- Again run the ```maven clean compile jib:build``` will build our app
- Then add the prometheus and grafana services in our main docker compose file
- Now lets create a prometheus config file "prometheus.yml"(with the scrape interval of 10s and the eval internal rules interval of 10s ..) and in the scrape config we ve to setup the jobs. in our case we ve 4 jobs/ services.

The prometheus serv running in port 9090 http://localhost:9090 
- in the service discovery we can see all our services 
# Grafana 

- The Grafana will provide us the UI Dashboard..
- This will also polls prometheus, so whenever there is a entry in the prometheus it will show the metrics in our Dashboard.
- Finally up and run our docker compose file with all the services configured 
- ```docker compose up -d```

The grafana is running in 3000 http://localhost:3000 cred (admin, password)
- To visualize the metrics grafana needs data source, we ve to add the data source.
- Add data source -> select prometheus -> give the name and the prometheus url(http://prometheus:9090), add and save.
- Then we can create our cust dashboard (or use the default one), import the dashboard config file --> "Grafana_Dashboard.json" and select the data source prometheus microservices.

Finally we can see our dashboard and we can switch b/w diff services and their metrics.
