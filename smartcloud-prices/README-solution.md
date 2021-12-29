# Design decisions
- The main flow in SmartcloudPriceService was structured in form of descriptive methods in for comprehension. This means more boilerplate as it requires 
additional lifting to Monads but I find it more readable when there are only 3 lines with descriptive names and no additional boilerplate (in for comprehension) such as here:
```commandline
      val result = for {
        uri      <- buildUri(kind)
        request  <- buildRequest(uri)
        response <- sendRequest(client, request)
      } yield response
```
Personally I find it very clean and readable, one look is enough to understand what the service does on highest level. It is always possible to 
go deeper into individual function implementation if required.
- To reduce boilerplate, instead of implementing decoders/encoders from scratch I switched to using Circe automatic 
derivation. To make it work also for value classes I had to downgrade Circe to last stable version to have Circe extras for the same version
- Treat most of the things in Server as resources to avoid performance issues e.g. with recreating HTTP client each time in underlying services, or creating multiple instances of it.
- All the "predicted" errors are handled by code in InstancePriceRoutes, while those unexpected are handled and mapped to 500 InternalServerError by http4s itself.
- Each error is logged for debugging. 

# Assumptions
- The response presented in task description returns something that is not really a json:
```
GET /prices?kind=sc2-micro
{"kind":"sc2-micro","amount":0.42}, ... (omitted)
```
It seems like it is an array of multiple objects, but it is not technically an array as brackets are missing ("[]").
Also the Smartcloud docker api returns only one instance in result, thus I assumed that the response
should not be an list, and contain price of only one machine.

- I didn't implement nor written tests for the InstanceKindService as the README.md states only to implement `SmartcloudPriceService` service


# Various ideas for future implementation
- Add tracking ID to each request to allow following logs flow when debugging
- Add some central logging e.g. Datadog/Sumologic/ELK or other.
- Add refined types instead of value classes
- Add automatic decoder/encoder creation through annotations
- Add some useful sbt plugins e.g. scapegoat/scalafix/sbt-updates/scoverage
- Add circuit breaker - it might be useful if underlying API has some quota
- Add some kind of retrying of requests

# Instruction on how to run code
- To run code locally first run docker as in steps in README.md and then run this project with: 
`sbt run`

example request:
```
curl --location --request GET 'localhost:8080/prices?kind=sc2-micro'
```

