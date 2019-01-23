# events-monitor

Tools, Documentation and Scripts to run the events monitoring tool

## dependencies

* npm
* ganache
* Kepper-contracts https://github.com/oceanprotocol/keeper-contracts
* Java 8
* Maven
* Docker (optional)

## Build

1. Start a test node with
    ```terminal
    $ganache-cli --deterministic
    ```
2. checkout and deploy contracts using
    ```terminal
    $git clone https://github.com/oceanprotocol/keeper-contracts
    $cd keeper-contracts
    $npm install
    $npx truffle compile
    ./scripts/deployContracts.sh
    ```
3. Start `eventeum` dependencies. It may take a couple of minutes for the system to be ready
    ```terminal
    docker-compose up -d
    ```
4. add connector with
    ```terminal
    curl -v POST http://localhost:8083/connectors  -d @connector.json --header  "Content-Type: application/json"
    ```
    If this step fails check if kafka connect is running. If you have a problem running the Elastic Search docker container check if vm.max_map_count is not set to low [65530] increase to at least [262144] with
    ```terminal
    $sudo sysctl -w vm.max_map_count=262144
    ```
    After that restart elastic search, kafka connect and kibana
5. Compile, test and package `eventeum`
    ```terminal
    $cd eventeum-0.4.0
    $mvn clean package
    ```
6. update `OceanMarket` address in `eventeum-0.4.0/server/application.yml` with the proxy address it can be taken from `zos.dev-*.json` under `"oceanprotocol/OceanMarket":` or from the terminal where the deployment script was executed.
7. run eventeum
    ```terminal
    $export SPRING_DATA_MONGODB_HOST=localhost:27017
    $export ETHEREUM_NODE_URL=http://localhost:8545
    $export ZOOKEEPER_ADDRESS=localhost:2181
    $export KAFKA_ADDRESSES=localhost:9092
    $cd server
    java -jar target/eventeum-server.jar
    ```
    At this point everything should be working as expected. You can check  the List kafka topics
    ```terminal
    $kafka-topics.sh --zookeeper zookeeper:2181 --list
    __consumer_offsets
    block-events
    connect-config
    connect-offsets
    connect-status
    contract-events
    filter-events
    filter-events-dlteventeum
    filter-events-erreventeum
    filter-eventsnull
    ```
    and you can listen to contract events with
    ```terminal
    $kafka-console-consumer.sh --zookeeper zookeeper:2181 --from-beginning --topic contract-events
    ```
You can now test kibana going to `localhost:5601` and create index pattern with
`contract-events`

You will se no events. To test events use truffle console from `keeper-contracts` folder as follow

```terminal
$npx truffle console
#get proxy
OceanMarket.deployed().then(i=>p=i)
#generate id
p.methods['generateId(string)']('test').then(i=>id=i)
#register, will trigger event
tx=p.register(id,1)
```