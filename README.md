# Play with DynamoDB using java sdk

## Instructions to run and test
(Tested with JDK 8 and sbt)

1. Clone git repository
2. Install and run DynamoDB on local machine (Instructions in bottom half of page: https://docs.aws.amazon.com/amazondynamodb/latest/gettingstartedguide/GettingStarted.JsShell.html)
3. If DynamoDB is not running on port 8000 (default), then edit value with endpoint URL in line `dynamodb.endpoint = "http://localhost:8000"` in `conf/application.conf`
4. Obtain AWS credentials to use AWS SDK for Java (Instructions: https://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide//java-dg-setup.html)
5. To run project on default port 9000, use a commandline to navigate to project, then enter command `sbt clean run`
6. To run tests, use a commandline to navigate to project, then enter command `sbt clean test`