# java-cli-utils
A collection of useful tools if you want to make a command line interface of some sorts.

## CliParser

This is a fluent wrapper over the Apache Commons CLI library.

### Usage

```java
Cli cli = CliParser
	.cliFor(args, "ServerBrowser", "a small tool to help validate some things")
    .addArg(Arg.String("server").shortName("s")
	.description("the server instance to connect to (http://<ip>.<port>)")
	.defaultValue(SERVER).optional())
	.addArg(Arg.String("user").shortName("u").description("the user to use when connecting to the server")
	.defaultValue(USER).optional())
	.addArg(Arg.String("password").shortName("p")
	.description("the password used when connecting to the server").defaultValue(PASSWORD)
	.optional())
	.addFlag(Flag.builder("list").shortName("l")
	.description("browses and lists all REST-API methods of this server instance"))
	.addMinRequired(1, "list").create();
if (cli.isHelpSet()) {
	System.exit(0);
}
String endpointUrl = cli.getArgValue("server");
String user = cli.getArgValue("user");
String password = cli.getArgValue("password");

if (cli.isFlagSet("list")) {
    // do something...
}
```

