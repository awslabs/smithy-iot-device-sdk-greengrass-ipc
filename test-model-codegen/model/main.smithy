// The smithy IDL file used for testing event-stream-rpc libraries. Changing the models here
// will aversely affect the expectation of unit tests in the package.

namespace awstest

@documentation("EchoTestRPC is a service for testing event-stream based clients, servers, and code generation.")
service EchoTestRPC {
    version: "2020-10-14",
    operations: [
        EchoMessage,
        EchoStreamMessages,
        CauseServiceError,
        CauseStreamServiceToError,
        GetAllProducts,
        GetAllCustomers,
    ]
}

//-----------Operations--------------------

@documentation("Returns the same data sent in the request to the response")
operation EchoMessage {
    input:  EchoMessageRequest,
    output: EchoMessageResponse
}

@documentation("Initial request and response are empty, but echos streaming messages sent by client")
operation EchoStreamMessages {
    input:  EchoStreamingRequest,
    output: EchoStreamingResponse
}

@documentation("Throws a ServiceError instead of returning a response.")
operation CauseServiceError {
    input:  CauseServiceErrorRequest,
    output: CauseServiceErrorResponse,
    errors: [ServiceError]
}

@documentation("Responds to initial request normally then throws a ServiceError on stream response")
operation CauseStreamServiceToError {
    input: EchoStreamingRequest,
    output: EchoStreamingResponse,
    errors: [ServiceError]
}

@documentation("Fetches all products, indexed by SKU")
operation GetAllProducts {
    input: GetAllProductsRequest,
    output: GetAllProductsResponse,
    errors: [ServiceError]
}

@documentation("Fetches all customers")
operation GetAllCustomers {
    input: GetAllCustomersRequest,
    output: GetAllCustomersResponse,
    errors: [ServiceError]
}
//-----------Shapes------------------------

@documentation("Data needed to perform an EchoMessage operation")
structure EchoMessageRequest {
    @documentation("Some message data")
    message: MessageData
}

@documentation("All data associated with the result of an EchoMessage operation")
structure EchoMessageResponse {
    @documentation("Some message data")
    message: MessageData
}

@documentation("Data needed to start an EchoStreaming streaming operation")
structure EchoStreamingRequest {
    @documentation("Some streaming message data")
    message: EchoStreamingMessage
}

@documentation("Data associated with the response to starting an EchoStreaming streaming operation")
structure EchoStreamingResponse {
    @documentation("Some streaming message data")
    message: EchoStreamingMessage
}

@documentation("Data needed to perform a CauseServiceError operation")
structure CauseServiceErrorRequest { }

@documentation("All data associated with the result of an EchoMessage operation")
structure CauseServiceErrorResponse { }

map StringToValue {
    key: String,
    value: Product
}

@documentation("Data associated with some notion of a message")
structure MessageData {
    @documentation("Some string data")
    stringMessage: String,

    @documentation("Some boolean data")
    booleanMessage: Boolean,

    @documentation("Some timestamp data")
    timeMessage: Timestamp,

    @documentation("Some document data")
    documentMessage: Document,

    @documentation("Some FruitEnum data")
    enumMessage: FruitEnum,

    @documentation("Some blob data")
    blobMessage: Blob,

    @documentation("Some list of strings data")
    stringListMessage: StringList,

    @documentation("A list of key-value pairs")
    keyValuePairList: KeyValuePairList,

    @documentation("A map from strings to Product shapes")
    stringToValue: StringToValue
}

@streaming
@documentation("A union of values related to a streaming message.  Only one field may bet set.")
union EchoStreamingMessage {
    @documentation("A message data record")
    streamMessage: MessageData,

    @documentation("A key value pair")
    keyValuePair: Pair
}

@documentation("Shape representing a pair of values")
structure Pair {
    @documentation("Pair.key as a string")
    @required
    key: String,

    @documentation("Pair.value also a string!")
    @required
    value: String
}

list StringList {
    member: String
}

list KeyValuePairList {
    member: Pair
}

@documentation("Data needed to perform a GetAllProducts operation")
structure GetAllProductsRequest {

}

@documentation("All data associated with the result of a GetAllProducts operation")
structure GetAllProductsResponse {
    @documentation("A map from strings to products")
    products: ProductMap
}

@documentation("Data needed to perform a GetAllCustomers operation")
structure GetAllCustomersRequest {

}

@documentation("A simple product definition")
structure Product {
    @documentation("The product's name")
    name: String,

    @documentation("How much the product costs")
    price: Float,
}

map ProductMap {
    key: String,
    value: Product
}

@documentation("All data associated with the result of a GetAllCustomers operation")
structure GetAllCustomersResponse {
    @documentation("A list of all known customers")
    customers: CustomerList
}

@documentation("A simple customer definition")
structure Customer {

    @documentation("Opaque customer identifier")
    id: Long,

    @documentation("First name of the customer")
    firstName: String,

    @documentation("Last name of the customer")
    lastName: String
}

list CustomerList {
    member: Customer
}

@enum([
    {
        value: "apl",
        name: "APPLE",
        documentation: "Apple documentation!"
    },
    {
        value: "org",
        name: "ORANGE",
        documentation: "Orange documentation!"
    },
    {
        value: "ban",
        name: "BANANA",
        documentation: "Banana documentation!"
    },
    {
        value: "pin",
        name: "PINEAPPLE",
        documentation: "Pineapple documentation!"
    }
])
@documentation("An enumeration of various tasty fruits.")
string FruitEnum

//----------errors----------------------

@error("server")
@documentation("A sample error shape")
structure ServiceError {

    @documentation("An error message")
    message: String,

    @documentation("Some auxiliary value")
    value: String
}

