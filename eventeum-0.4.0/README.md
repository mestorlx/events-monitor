# Eventeum
A bridge between your Ethereum smart contract events and backend microservices.  Eventeum listens for specified event emissions from the Ethereum network, and broadcasts these events into your middleware layer.  This provides a distinct seperation of concerns and means that your microservices do not have to subscribe to events directly to an Ethereum node.

**Master**

[![CircleCI](https://circleci.com/gh/ConsenSys/eventeum/tree/master.svg?style=svg)](https://circleci.com/gh/ConsenSys/eventeum/tree/master)

**Development**
  
[![CircleCI](https://circleci.com/gh/ConsenSys/eventeum/tree/development.svg?style=svg)](https://circleci.com/gh/ConsenSys/eventeum/tree/development)

## Features
* Dynamically Configurable - Eventeum exposes a REST api so that smart contract events can be dynamically subscribed / unsubscribed.

* Highly Available - Eventeum instances communicate with each other to ensure that every instance is subscribed to the same collection of smart contract events.

* Resilient - Node failures are detected and event subscriptions will continue from the failure block once the node comes back online.

* Fork Tolerance - Eventeum can be configured to wait a certain amount of blocks before an event is considered 'Confirmed'.  If a fork occurs during this time, a message is broadcast to the network, allowing your services to react to the forked/removed event.

## Supported Broadcast Mechanisms
* Kafka
* HTTP Post
* [RabbitMQ](https://www.rabbitmq.com/)

## Getting Started
Follow the instructions below in order to run Eventeum on your local machine for development and testing purposes.

### Prerequisites
* Java 8
* Maven
* Docker (optional)

### Build
1. After checking out the code, navigate to the root directory
```
$ cd /path/to/eventeum/
```

2. Compile, test and package the project
```
$ mvn clean package
```

### Run

a. If you have a running instance of MongoDB, Kafka, Zookeeper and an Ethereum node:

**Executable JAR:**
```
$ cd server
$ export SPRING_DATA_MONGODB_HOST=<mongodb-host:port>
$ export ETHEREUM_NODE_URL=http://<node-host:port>
$ export ZOOKEEPER_ADDRESS=<zookeeper-host:port>
$ export KAFKA_ADDRESSES=<kafka-host:port>
$ export RABBIT_ADDRESSES=<rabbit-host:port>

$ java -jar target/eventeum-server.jar
```

**Docker:**

```
$ cd server
$ docker build  . -t kauri/eventeum:latest

$ export SPRING_DATA_MONGODB_HOST=<mongodb-host:port>
$ export ETHEREUM_NODE_URL=http://<node-host:port>
$ export ZOOKEEPER_ADDRESS=<zookeeper-host:port>
$ export KAFKA_ADDRESSES=<kafka-host:port>
$ export RABBIT_ADDRESSES=<rabbit-host:port>

$ docker run -p 8060:8060 kauri/eventeum
```

b. If you prefer build an all-in-one test environment with a parity dev node, use docker-compose:
```
$ cd server
$ docker-compose -f docker-compose.yml build
$ docker-compose -f docker-compose.yml up
```

## Registering Events

### REST
Eventeum exposes a REST api that can be used to register events that should be subscribed to / broadcast.

-   **URL:** `/api/rest/v1/event-filter`    
-   **Method:** `POST`
-   **Headers:**  

| Key | Value | 
| -------- | -------- |
| content-type | application/json |

-   **URL Params:** `N/A`
-   **Body:** 
```
{
	"id": "event-identifier",
	"contractAddress": "0x1fbBeeE6eC2B7B095fE3c5A572551b1e260Af4d2",
	"eventSpecification": {
		"eventName": "TestEvent",
		"indexedParameterDefinitions": [
		  {"position": 0, "type": "UINT256"},
		  {"position": 1, "type": "ADDRESS"}],
		"nonIndexedParameterDefinitions": [
		  {"position": 2, "type": "BYTES32"},
		  {"position": 3, "type": "STRING"}] },
	"correlationIdStrategy": {
		"type": "NON_INDEXED_PARAMETER",
		"parameterIndex": 0 }
}
```
| Name | Type | Mandatory | Default | Description |
| -------- | -------- | -------- | -------- | -------- |
| id | String | no | Autogenerated | A unique identifier for the event. |
| contractAddress | String | yes |  | The address of the smart contract that the address will be emitted from. |
| eventSpecification | json | yes |  | The event specification |
| correlationIdStrategy | json | no | null | Define a correlation id for the event (only used with the Kafka broadcaster).  See the advanced section for details. |

**eventSpecification**:

| Name | Type | Mandatory | Default | Description |
| -------- | -------- | -------- | -------- | -------- |
| eventName | String | yes | | The event name within the smart contract |
| indexedParameterTypes | String array | no | null | The array of indexed parameter types for the event. |
| nonIndexedParameterTypes | String array | no | null | The array of non-indexed parameter types for the event. |

**parameterDefinition**:

| Name | Type | Mandatory | Default | Description |
| -------- | -------- | -------- | -------- | -------- |
| position | Number | yes | | The zero indexed position of the parameter within the event specification |
| type | String | yes | | The type of the event parameter. |

Currently supported parameter types: UINT256, ADDRESS, BYTES32, STRING

**correlationIdStrategy**:

| Name | Type | Mandatory | Default | Description |
| -------- | -------- | -------- | -------- | -------- |
| type | String | yes | | The correlation id strategy type. |
| parameterIndex | Number | yes | | The parameter index to use within the correlation strategy. |

-   **Success Response:**
    -   **Code:** 200  
        **Content:** 
```
{
    "id": "event-identifier"
}
```

### Hard Coded Configuration
Static events can be configured within the application.yml file of Eventeum.

```
eventFilters:
  - id: RequestCreated
    contractAddress: ${CONTRACT_ADDRESS:0x4aecf261541f168bb3ca65fa8ff5012498aac3b8}
    eventSpecification:
      eventName: RequestCreated
      indexedParameterDefinitions:
        - position: 0
          type: BYTES32
        - position: 1
          type: ADDRESS
      nonIndexedParameterDefinitions:
        - position: 2
          type: BYTES32
    correlationId:
      type: NON_INDEXED_PARAMETER
      index: 0
```

## Un-Registering Events

### REST

-   **URL:** `/api/rest/v1/event-filter/{event-id}`    
-   **Method:** `DELETE`
-   **Headers:**  `N/A`
-   **URL Params:** `N/A`
-   **Body:** `N/A`

-   **Success Response:**
    -   **Code:** 200  
        **Content:** `N/A`
	
## Broadcast Messages Format

###  Contract Events
When a subscribed event is emitted, a JSON message is broadcast to the configured kafka topic or rabbit exchange (contract-events by default), with the following format:

```
{
	"id":"unique-event-id",
	"type":"CONTRACT_EVENT",
	"details":{
		"name":"DummyEvent",
		"filterId":"63da468c-cec6-49aa-bea4-eeba64fb1df4",
		"indexedParameters":[{"type":"bytes32","value":"BytesValue"},
			{"type":"address","value":"0x00a329c0648769a73afac7f9381e08fb43dbea72"}],
		"nonIndexedParameters":[{"type":"uint256","value":10},
			{"type":"string","value":"StringValue"}],
		"transactionHash":"0xe4fd0f095990ec471cdf40638336a73636d2e88fc1a240c20b45101b9cce9438",
		"logIndex":0,
		"blockNumber":258,
		"blockHash":"0x65d1956c2850677f75ec9adcd7b2cfab89e31ad1e7a5ba93b6fad11e6cd15e4a",
		"address":"0x9ec580fa364159a09ea15cd39505fc0a926d3a00",	
		"status":"UNCONFIRMED",
		"eventSpecificationSignature":"0x46aca551d5bafd01d98f8cadeb9b50f1b3ee44c33007f2a13d969dab7e7cf2a8",
		"id":"unique-event-id"},
		"retries":0
}

```

### Block Events
When a new block is mined, a JSON message is broadcast to the configured kafka topic or rabbit exchange (block-events by default), with the following format:

```
 {
 	"id":"0x79799054d1782eb4f246b3055b967557148f38344fbd7020febf7b2d44faa4f8",
	"type":"BLOCK",
	"details":{
		"number":257,
		"hash":"0x79799054d1782eb4f246b3055b967557148f38344fbd7020febf7b2d44faa4f8",
		"timestamp":12345678},
	"retries":0
}
```

## Configuration
Many values within Eventeum are configurable either by changing the values in the application.yml file or by setting the associated environment variable.

| Env Variable | Default | Description |
| -------- | -------- | -------- |
| SERVER_PORT | 8060 | The port for the eventeum instance. |
| ETHEREUM_BLOCKSTRATEGY | POLL | The strategy for obtaining block events from an ethereum node (POLL or PUBSUB) |
| ETHEREUM_NODE_URL | http://localhost:8545 | The ethereum node url. |
| ETHEREUM_NODE _HEALTHCHECK_POLLINTERVAL | 2000 | The interval time in ms, in which a request is made to the ethereum node, to ensure that the node is running and functional. |
| EVENTSTORE_TYPE | DB | The type of eventstore used in Eventeum. (See the Advanced section for more details) |
| BROADCASTER_TYPE | KAFKA | The broadcast mechanism to use.  (KAFKA or HTTP or RABBIT) |
| BROADCASTER_CACHE _EXPIRATIONMILLIS | 6000000 | The eventeum broadcaster has an internal cache of sent messages, which ensures that duplicate messages are not broadcast.  This is the time that a message should live within this cache. |
| BROADCASTER_EVENT _CONFIRMATION _NUMBLOCKSTOWAIT | 12 | The number of blocks to wait (after the initial mined block) before broadcasting a CONFIRMED event |
| BROADCASTER_EVENT _CONFIRMATION _NUMBLOCKSTOWAITFORMISSINGTX | 200 | After a fork, a transaction may disappear, and this is the number of blocks to wait on the new fork, before assuming that an event emitted during this transaction has been INVALIDATED |
| BROADCASTER_MULTIINSTANCE | false | If multiple instances of eventeum are to be deployed in your system, this should be set to true so that the eventeum communicates added/removed filters to other instances, via kafka. |
| BROADCASTER_HTTP CONTRACTEVENTSURL | | The http url for posting contract events (for HTTP broadcasting) |
| BROADCASTER_HTTP BLOCKEVENTSURL | | The http url for posting block events (for HTTP broadcasting) |
| ZOOKEEPER_ADDRESS | localhost:2181 | The zookeeper address |
| KAFKA_ADDRESSES | localhost:9092 | Comma seperated list of kafka addresses |
| KAFKA_TOPIC_CONTRACT_EVENTS | contract-events | The topic name for broadcast contract event messages |
| KAFKA_TOPIC_BLOCK_EVENTS | block-events | The topic name for broadcast block event messages |
| SPRING_DATA_MONGODB_HOST | localhost | The mongoDB host (used when event store is set to DB) |
| SPRING_DATA_MONGODB_PORT | 27017 | The mongoDB post (used when event store is set to DB) |
| RABBIT_ADDRESS | localhost:5672 | property spring.rabbitmq.host (The rabbitmq address) |
| RABBIT_EXCHANGE | ThisIsAExchange | property rabbitmq.exchange |
| RABBIT_ROUTING_KEY | thisIsRoutingKey | property rabbitmq.routingKeyPrefix |

### INFURA Support Configuration
Connecting to an INFURA node is only supported if connecting via websockets (`wss://<...>` node url).  The blockstrategy must also be set to PUBSUB.

## Advanced
### Correlation Id Strategies (Kafka Broadcasting)

Each subscribed event can have a correlation id strategy association with it, during subscription.  A correlation id strategy defines what the kafka message key for a broadcast event should be, and allows the system to be configured so that events with particular parameter values are always sent to the same partition.

Currently supported correlation id strategies are:

**Indexed Parameter Strategy** - An indexed parameter within the event is used as the message key when broadcasting.
**Non Indexed Parameter Strategy** - An non-indexed parameter within the event is used as the message key when broadcasting.

### Event Store

Eventeum utilises an event store in order to establish the block number to start event subscriptions from, in the event of a failover.  For example, if the last event broadcast for event with id X had a block number of 123, then on a failover, eventeum will subscribe to events from block 124.

There are currently 2 supported event store implementations:

#### MongoDB

Broadcast events are saved and retrieved from a mongoDB database.

**Required Configuration**

| Env Variable | Default | Description |
| -------- | -------- | -------- |
| EVENTSTORE_TYPE | DB | MongoDB event store enabled |
| SPRING_DATA_MONGODB_HOST | localhost | The mongoDB host |
| SPRING_DATA_MONGODB_PORT | 27017 | The mongoDB post |

#### REST Service

Eventeum polls an external REST service in order to obtain a list of events broadcast for a specific event specification.  It is assumed that this REST service listens for broadcast events on the kafka topic and updates its internal state...broadcast events are not directly sent to the REST service by eventeum.

The implemented REST service should have a pageable endpoint which accepts a request with the following specification:

-   **URL:** Configurable, defaults to `/api/rest/v1/event`    
-   **Method:** `GET`
-   **Headers:**  

| Key | Value | 
| -------- | -------- |
| content-type | application/json |

-   **URL Params:** 

| Key | Value | 
| -------- | -------- |
| page | The page number |
| size | The page size |
| sort | The results sort field |
| dir | The results sort direction |
| signature | Retrieve events with the specified event signature |

-   **Body:** `N/A`

-   **Success Response:**
    -   **Code:** 200  
        **Content:** 
```
{
	"content":[
		{"blockNumber":10,"id":<unique event id>}],
	"page":1,
	"size":1,
	"totalElements":1,
	"first":false,
	"last":true,
	"totalPages":1,
	"numberOfElements":1,
	"hasContent":true
}
```

**Required Configuration**

| Env Variable | Default | Description |
| -------- | -------- | -------- |
| EVENTSTORE_TYPE | REST | REST event store enabled |
| EVENTSTORE_URL  | http://localhost:8081/api/rest/v1 | The REST endpoint url |
| EVENTSTORE_EVENTPATH | /event | The path to the event REST endpoint |

### Integrating Eventeum into Third Party Spring Application

Eventeum can be embedded into an existing Spring Application via an annotation.

#### Steps to Embed

1. Add the Consensys Kauri bintray repository into your `pom.xml` file:

```
<repositories>
  <repository>
    <id>bintray-consensys-kauri</id>
    <url>https://consensys.bintray.com/kauri</url>
  </repository>
</repositories>
```

2. Add the eventeum-core dependency to your `pom.xml` file:

```
<dependency>
  <groupId>net.consensys.eventeum</groupId>
  <artifactId>eventeum-core</artifactId>
  <version>*LATEST_EVENTEUM_VERSION*</version>
</dependency>
```

3. Within your Application class or a `@Configuration` annotated class, add the `@EnableEventeum` annotation.

## Known Caveats / Issues
* In multi-instance mode, where there is more than one Eventeum instance in a system, your services are required to handle duplicate messages gracefully, as each instance will broadcast the same events.