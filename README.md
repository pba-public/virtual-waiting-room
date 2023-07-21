# virtual-waiting-room

## Goal
An POC of virtual waiting room to handle natural burst of trafic on websites

## Technical stack
* Scala
* Redis
* ZIO ecosystem (ZIO Http, ZIO Redis, ZIO logging, ...)
* Kamon

## How to test
At first, start up the Redis:
```sh
docker-compose up
```

Then start the application:
```sh
sbt run
```

Finally you can launch the automated tests:
```sh
sbt test
```

Or call directly the endpoints of the API:
### Init an event
```sh
curl --location --verbose --request GET 'http://localhost:8080/init-event?event_id=001&capacity=10'
```

The expected response is a 200 return code

### Assign a queue number to a request
```sh
curl --location --verbose --request GET 'http://localhost:8080/assign-queue-num?event_id=001'
```

The expected response is a 200 return code with a body:
```json
{
    "request_id": "71af576b-fcf2-491e-bb8d-08b5e605fc48",
    "queue_num": 1
}
```

### Fetch the current serving number
```sh
curl --location --verbose --request GET 'http://localhost:8080/serving-num?event_id=001'
```

The expected response is a 200 return code

### Increment serving number
```sh
curl --location --verbose --request GET 'http://localhost:8080/increment-serving-num?event_id=id001&increment=10'
```

The expected response is a 200 return code

### Generate a token for a request
```sh
curl --location --verbose --request GET 'http://localhost:8080/generate-token?event_id=id006&request_id=71af576b-fcf2-491e-bb8d-08b5e605fc48'
```

The expected response is a 200 return code with a body:
```text
2e864cc5-e741-483e-aa6c-359fd3756af5
```

### Health check
```sh
curl --location --request GET 'http://localhost:8080/healthcheck'
```
The health check simply set a key in the Redis database. So the response to the check is binary (OK or KO).
The expected response is a 200 return code

## Observability
A Prometheus endpoint is deployed on port 9095 (ex: http://localhost:9095/metrics). It allows to fetch a counter metric "wr.api.counter" with tags status, domain. It exposes also JVM metrics.
