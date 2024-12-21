<h1 align="center">Spring Requery</h1>

<p align="center">
    <img src="./codexio-logo.png" width="555" height="90"/>
    <br/>
    <em>
        May your queries be requested.
    </em>
</p>

<div align="center">

[![Maven Central](https://img.shields.io/maven-central/v/bg.codexio.springframework.data.jpa.requery/requery-core?color=EE5A9C)](https://central.sonatype.com/artifact/bg.codexio.springframework.data.jpa.requery/requery-core)
[![Build](https://github.com/CodexioLtd/spring-requery/actions/workflows/maven.yml/badge.svg)](https://github.com/CodexioLtd/spring-requery/actions/workflows/maven.yml)
[![Coverage](https://codecov.io/github/CodexioLtd/spring-requery/graph/badge.svg?token=013OEUIYWI)](https://codecov.io/github/CodexioLtd/spring-requery)
[![License](https://img.shields.io/github/license/CodexioLtd/spring-requery.svg)](./LICENSE)

</div>

<hr/>

## Preambule

Have you ever encountered a situation where your Spring REST resources needed to be queried
in tons of different ways? Making you provide a massive number of Repository query methods,
and most probably different endpoints. Or even a bunch of `@RequestParam`'s and combined in
`COALESCE` calls. **Requery** aims to solve this struggle by providing a conversion between
a generic HTTP Request and a desired query while maintaining the security underneath.

## Table of Contents

* [Preambule](#preambule)
* [Table of Contents](#table-of-contents)
* [Features](#features)
* [Quick Start](#quick-start)
* [Usage](#usage)
    * [Argument Resolver Configuration](#argument-resolver-configuration)
    * [Hibernate Dialect Configuration](#hibernate-dialect-configuration)
    * [Basic Usage in a Controller](#basic-usage-in-a-controller)
* [Filtering Options](#filtering-options)
    * [Sample Java Entity](#sample-java-entity)
    * [Supported Filter Operations](#supported-filter-operations)
    * [Simple Filter Examples](#simple-filter-examples)
        * [Single Condition Simple Filter](#single-condition-simple-filter)
        * [Multiple Conditions Simple Filter](#multiple-conditions-simple-filter)
    * [Complex Filter Examples](#complex-filter-examples)
        * [Filter Structure Overview](#filter-structure-overview)
        * [Properties](#properties)
        * [Sample complex filter JSON](#sample-complex-filter-json)
* [Contributing](#contributing)
* [License](#license)

## Features

* **Dynamic Query Generation:** Create complex queries from HTTP requests dynamically.
* **Flexible Filtering:** Use the provided `FilterJsonArgumentResolver` to handle JSON based filtering.
* **Enum Handling:** Map enums to database columns seamlessly with `JoinColumnEnumeration`.

## Quick Start

1. Begin by setting up a Spring MVC project with Maven
2. Add the **Requery** library as dependency in your project and follow the [Usage section](#usage)'s instructions.

```xml

<dependency>
    <groupId>bg.codexio.springframework.data.jpa.requery</groupId>
    <artifactId>requery-core</artifactId>
    <version>1.0.5</version>
</dependency>
```

3. Use filter in your controller

```java

@GetMapping("/users")
public ResponseEntity<List<User>> getMyEntities(Specification<User> specification) {
    return ResponseEntity.ok(userRepository.findAll(spec));
}
```

4. Example usage
   Now that your project is configured, you can start using the filter functionality. Below is an example of how a
   client might use the filter functionality to query your API

**Filter Users by first name**
To filter users whose first name begins with "John", construct the following filter. Remember, the JSON must be
URL-encoded:

```json
{
  "field": "firstName",
  "value": "John",
  "operation": "BEGINS_WITH_CASEINS"
}
```

When properly encoded the request will look like this:

```
GET /users?filter=%7B%22field%22%3A%22firstName%22%2C%22value%22%3A%22John%22%2C%22operation%22%3A%22BEGINS_WITH%22%7D
```

You can send this request from any HTTP client or browser, and the server should return a list of users that match the
filter criteria.

This example assumes that your server is correctly set up to receive requests at `/users` and that you have a User
repository with the findAll method capable of interpreting `Specification<User>` objects.

For more complex filtering examples, please see the [Filtering options](#filtering-options) section.

## Usage

**Requery** is designed to simplify the dynamic generation of queries from HTTP requests in Spring MVC applications.
Here’s how you can integrate and utilize the key components of the library:

### Argument Resolver Configuration

To leverage the `FilterJsonArgumentResolver` and other components, you need to set up several beans in your Spring
configuration:

#### Create the argument resolver:

To configure the instantiation of the `FilterJsonArgumentResolver` create a bean in your configuration class like this:

```java

@Bean
public FilterJsonTypeConverter filterJsonTypeConverter() {
    return new FilterJsonTypeConverterImpl();
}

@Bean
public FilterJsonArgumentResolver filterJsonArgumentResolver(List<HttpFilterAdapter> activeAdapters) {
    return new FilterJsonArgumentResolver(
            filterJsonTypeConverter(),
            activeAdapters
    );
}
```

#### Register the argument resolver:

In order to register the newly created `FilterJsonArgumentResolver` create the following class:

```java

@Configuration
public class FilterJsonArgumentResolverConfiguration
        implements WebMvcConfigurer {
    private final FilterJsonArgumentResolver filterJsonArgumentResolver;

    public FilterJsonArgumentResolverConfiguration(FilterJsonArgumentResolver filterJsonArgumentResolver) {
        this.filterJsonArgumentResolver = filterJsonArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this.filterJsonArgumentResolver);
    }
}
```

### Default Adapter Bean creation

Then in order to register the default `HttpFilterAdapter` implementation - the `JsonHttpFilterAdapter` - you will have
to add a Bean in your configuration class and add another bean as constructor parameter of type `ObjectMapper`

```java

@Bean
@ConditionalOnMissingBean
public ObjectMapper objectMapper() {
    return new ObjectMapper();
}

@Bean
public HttpFilterAdapter httpFilterAdapter() {
    return new JsonHttpFilterAdapter(objectMapper());
}
```

#### There is a possibility to create and instantiate a custom FilterAdapter

All you would have to do is to create your class implementing the `HttpFilterAdapter` and annotate it as a `@Component`.
Spring will automatically include it in the list passed in the constructor of the `FilterJsonArgumentResolver` and based
on the request and the supports method you will have a second working adapter.

```java

@Component
public class CustomHttpFilterImpl
        implements HttpFilterAdapter {
    @Override
    public boolean supports(HttpServletRequest request) {
        //your implementation
        return true;
    }

    @Override
    public <T> FilterRequestWrapper<T> adapt(HttpServletRequest webRequest) {
        //your implementation
        return new FilterRequestWrapper<>();
    }
}
```

### Hibernate Dialect Configuration

Since `Requery` may optimize SQL queries differently based on the database type. Currently, we support two dialect
extensions - MySQL and PostgreSQL

Add the Hibernate dialect setting in your `application.properties` or `application.yml` as follows:

#### For MySQL:

```properties
spring.jpa.properties.hibernate.dialect=bg.codexio.springframework.data.jpa.requery.dialect.RequeryEnhancedMySQLDialect
```

#### For PostgreSQL:

```properties
spring.jpa.properties.hibernate.dialect=bg.codexio.springframework.data.jpa.requery.dialect.RequeryEnhancedPostgreSQLDialect
```

### Basic Usage in a Controller

After setting up your configuration, you can use the `FilterJsonArgumentResolver` in your controllers to dynamically
construct queries based on JSON input:

```java

@RestController
public class MyController {

    private final MyEntityRepository myEntityRepository;

    public MyController(MyEntityRepository myEntityRepository) {
        this.myEntityRepository = myEntityRepository;
    }

    @GetMapping("/my-entities")
    public ResponseEntity<List<MyEntity>> getMyEntities(Specification<MyEntity> spec) {
        return ResponseEntity.ok(myEntityRepository.findAll(spec));
    }
}
```

## Filtering Options

This section provides detailed examples of both simple and complex filters that you can apply using our filtering
system. These examples will help you utilize our library to perform dynamic queries effectively.

**Disclaimer!** For simplicity the examples include JSON format filters, but ensure the requests are properly URL
encoded.

### Sample Java Entity

Here’s a typical Java entity class representing a `User`. This class will be referenced in the filter examples to give
users a clear context.

```java

@Entity
public class User {

    @Id
    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private Integer age;

    // Constructors, getters, and setters
}
```

### Supported Filter Operations

Currently, **Requery** has support for the following operations:

* `EQ` - Equals
* `GT` - Greater Than
* `GTE` - Greater Than or Equal To
* `LT` - Less Than
* `LTE` - Less Than or Equal To
* `BEGINS_WITH` - Begins With (case-sensitive)
* `ENDS_WITH` - Ends With (case-sensitive)
* `CONTAINS` - Contains substring (case-sensitive)
* `IN` - Checks if the field's value is within the provided list of values
* `NOT_IN` - Checks if the field's value is not within the provided list of values
* `BEGINS_WITH_CASEINS` - Begins With (case-insensitive)
* `ENDS_WITH_CASEINS` - Ends With (case-insensitive)
* `CONTAINS_CASEINS ` - Contains (case-insensitive)
* `EMPTY` - Checks if the field's value is *null*
* `NOT_EMPTY` - Checks if the field's value is not *null*

### Simple Filter Examples

Simple filters allow for direct, condition-based querying on entity attributes. These filters are straightforward and
are used for single or multiple condition checks on the data without nesting or logical operators between different
conditions.

#### Single Condition Simple Filter

A single condition simple filter applies one filter criterion to an entity. For example, to find all users with the
first name "John":

```json
{
  "field": "firstName",
  "operation": "EQ",
  "value": "John"
}
```

#### Multiple Conditions Simple Filter

You can also apply multiple conditions in a simple filter without using logical operators between them. This is
essentially an array of simple filters where each condition is evaluated independently, and the overall result is the
aggregation of all conditions. Here's an example of a simple filter with multiple conditions:

```json
[
  {
    "field": "lastName",
    "operation": "CONTAINS",
    "value": "Doe"
  },
  {
    "field": "age",
    "operation": "GTE",
    "value": 25
  }
]
```

### Complex Filter Examples

#### Filter Structure Overview

Our complex filtering system allows for the construction of sophisticated queries with multiple layers of logical
operations. The JSON for a complex filter contains several key properties:

* **groupOperations**: An array of filter conditions.
* **nonPriorityGroupOperators**: An array of logical operators that define how the results of `groupOperations` are
  combined.
* **rightSideOperands**: An object specifying additional nested filter conditions and the logical operator to apply with
  the previous level.

#### Properties

* **groupOperations**
    * Array of objects where each object represents a filtering condition.
    * Each object in the array has the following attributes:
        * **field**: The attribute of the entity on which the filter is applied.
        * **operation**: The type of operation (EQ, IN, GTE, etc.).
        * **value**: The value against which the entity’s attribute is compared.
* **nonPriorityGroupOperators**
    * An array of strings representing logical operators (AND, OR)
    * These operators are applied sequentially between the conditions defined in `groupOperations`.
* **rightSideOperands**
    * An object that allows nesting additional filters with a logical operator specifying how this nested filter
      combines with the previous conditions.
    * Contains the following sub-properties:
        * **unaryGroupOperator**: A logical operator (AND, OR) that applies to the nested group relative to the previous
          conditions.
        * **unaryGroup**: An object that mimics the structure of the top-level filter, allowing recursive nesting of
          conditions. It contains its own `groupOperations`, `nonPriorityGroupOperators`, and potentially another
          `rightSideOperands`.

#### Sample complex filter JSON

```json
{
  "groupOperations": [
    {
      "field": "email",
      "operation": "CONTAINS",
      "value": "example.com"
    }
  ],
  "nonPriorityGroupOperators": [
    "AND"
  ],
  "rightSideOperands": {
    "unaryGroupOperator": "OR",
    "unaryGroup": {
      "groupOperations": [
        {
          "field": "firstName",
          "operation": "IN",
          "value": [
            "John",
            "Jane"
          ]
        },
        {
          "field": "lastName",
          "operation": "BEGINS_WITH_CASEINS",
          "value": "Doe"
        }
      ],
      "nonPriorityGroupOperators": [
        "OR"
      ],
      "rightSideOperands": {
        "unaryGroupOperator": "AND",
        "unaryGroup": {
          "groupOperations": [
            {
              "field": "age",
              "operation": "GT",
              "value": 25
            }
          ],
          "nonPriorityGroupOperators": []
        }
      }
    }
  }
}
```

This setup enables your Spring application to interpret JSON filter parameters directly from request queries and convert
them into JPA specifications

## Contributing

This project could use a support and contributors are very welcomed. If you feel that something has to be
changed or a bug to be fixed, you can report a [new issue](https://github.com/CodexioLtd/spring-requery/issues/new), and
we can take care of it.

If you want to submit directly a code fix, we will be more than glad to see it. Fork the repository and start a clean
branch out of the version you want to patch. When you are finished, make sure all your tests are passing and the
coverage remains in decent level by executing `mvn clean test jacoco:report -Pmvn-deploy`.

Please use the [code style](./codestyle.xml)
in the project root folder. If your IDE does not support it, we strongly encourage you just to follow
the code styling in the rest of the classes and methods.

After all, your tests are passing and the coverage seems good to you, create a
[pull request](https://github.com/CodexioLtd/spring-requery/compare). We will review the request and either leave
some meaningful suggestions back or maybe merge it and release it with the next release.

...

## License

Copyright 2024 [Codexio Ltd.](https://codexio.bg)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
