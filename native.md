# Developer Setup

WCST-Services API requires:

- JDK 1.8 and above
- Maven

## Base Setup

To start, fetch this code:

`git clone https://github.com/atlas-brownie/WCST-Services.git`

1. Run maven to install dependencies and package
   - `mvn clean install`
1. Go to the file src/main/resources/application-local.yaml and update following properties
   - vaAuthHeaderValue ( with the apikey value obtained from [VA developer site](https://developer.va.gov/apply))
   - vaClaimIntakePointerUrl (Make sure it is pointing to sandbox box environment)
1. Run spring boot application.
   - `mvn spring-boot:run`
1. Verify deployment by using [swagger end point](http://localhost:8080/swagger-ui.html)
1. Execute Claims Controller in swagger.
