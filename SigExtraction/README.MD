## SigDataExtraction

This daemon processes the body of a mail to find its signature part, and extract from that sender's data.

### Prerequisites

You will need to download the library currently used to realise the classification [libsmv](https://www.csie.ntu.edu.tw/~cjlin/libsvm/). You will also need to add to the build path the [coreNLP](https://stanfordnlp.github.io/CoreNLP/) library to perform the Named Entity Recognition used in this project.

### Usage

The function Index.collect(String mail) will extract data from a body's mail and set predictions in the API after checking that it's not part of the exclusion list. It should be given a json with fields sender, recipients and text.


You can also use Index.extractDataFromMail(String text) which returns an object DataCollected with get methods to retrieve data later.

### Testing
