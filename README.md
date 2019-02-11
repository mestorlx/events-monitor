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
4. Force elastic search mapping by creating the index with
   ```terminal
   curl -X PUT http://localhost:9200/contract-events  -d @createIndex.json --header  "Content-Type: application/json"
   ```
5. add connector with
    ```terminal
    curl -v POST http://localhost:8083/connectors  -d @connector.json --header  "Content-Type: application/json"
    ```
    If this step fails check if kafka connect is running. If you have a problem running the Elastic Search docker container check if vm.max_map_count is not set to low [65530] increase to at least [262144] with
    ```terminal
    $sudo sysctl -w vm.max_map_count=262144
    ```
    After that restart elastic search, kafka connect and kibana
6. Add a kibana dashboard. This sample dashboard will show an histogram of events emitted.
    ```terminal
    $curl -v POST localhost:5601/api/kibana/dashboards/import  -H 'kbn-xsrf:true' --header 'Content-type:application/json' -d @visualization.json
    ```
7. Compile, test and package `eventeum`
    ```terminal
    $cd eventeum-0.4.0
    $mvn clean package
    ```
8. update `KeeperContracts` addresses in `eventeum-0.4.0/server/application.yml` with the proxy address it can be taken from `zos.dev-*.json` under `"oceanprotocol/ContractName":` or from the terminal where the deployment script was executed.
9.  run eventeum
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
You can now test kibana going to `localhost:5601`

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