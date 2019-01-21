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
    ```console
    $ganache-cli --deterministic
    ```
2. checkout and deploy contracts using
    ```console
    $git clone https://github.com/oceanprotocol/keeper-contracts
    $cd keeper-contracts
    $npm install
    $npx truffle compile
    ./scripts/deployContracts.sh
    ```
3. Start eventeum dependencies. It may take a couple of minutes for the system to be ready
    ```console
    docker-compose up -d
    ```
4. add connector with
    ```console
    curl -v POST http://localhost:8083/connectors  -d @connector.json --header  "Content-Type: application/json"
    ```
    If this step fails check if kafka connect is running
5. Compile, test and package `eventeum`
    ```console
    $cd eventeum-0.4.0
    $mvn clean package
    ```
6. update `OceanMarket` address in `eventeum-0.4.0/server/application.yml` with the proxy address it can be taken from `zos.dev-*.json` under `"oceanprotocol/OceanMarket":` or from the terminal where the deployment script was executed.
7. run eventeum
    ```console
    $export SPRING_DATA_MONGODB_HOST=localhost:27017
    $export ETHEREUM_NODE_URL=http://localhost:8545
    $export ZOOKEEPER_ADDRESS=localhost:2181
    $export KAFKA_ADDRESSES=localhost:9092
    $cd server
    java -jar target/eventeum-server.jar
    ```
At this point everything should be working as expected. You can now test kibana going to `localhost:5601` and create index pattern with
`contract-events`

You will se no events. To test events use truffle console from `keeper-contracts` folder as follow

```console
$npx truffle console
#get proxy
OceanMarket.deployed().then(i=>p=i)
#generate id
p.methods['generateId(string)']('test').then(i=>id=i)
#register, will trigger event
tx=p.register(id,1)
```