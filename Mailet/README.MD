## Mailet

This mailet is used to retrieve the sender, the recipients and the body of a mail, and send them as the payload in a RabbitMQ Channel. 

Data is sent in json with fields sender, recipients and text. 

### Prerequisites

This mailet is supposed to be integrated in [JAMES](https://james.apache.org/).

### Installation
Download the folder.

Add the mailet file at [james-project/mailet/standard/src/main/java/org/apache/james/transport/mailets/](https://github.com/apache/james-project/tree/407036b0036568ef2d7a3fe031fb325d79837b5a/mailet/standard/src/main/java/org/apache/james/transport/mailets)


### Testing
A unit test and an integration tests are also given in the folder. 

They can be added respectively at [james-project/mailet/standard/src/test/java/org/apache/james/transport/mailets/](https://github.com/apache/james-project/tree/407036b0036568ef2d7a3fe031fb325d79837b5a/mailet/standard/src/test/java/org/apache/james/transport/mailets) and [james-project/server/mailet/integration-testing/src/test/java/org/apache/james/transport/mailets/](https://github.com/apache/james-project/tree/407036b0036568ef2d7a3fe031fb325d79837b5a/server/mailet/integration-testing/src/test/java/org/apache/james/transport/mailets)