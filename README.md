# Scientist4J

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.rawls238/Scientist4J/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.rawls238/Scientist4J)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.github.rawls238/Scientist4J/badge.svg)](http://www.javadoc.io/doc/com.github.rawls238/Scientist4J)

A port of Github's refactoring tool [Scientist](https://github.com/github/scientist) in Java

# Installation

```java
<dependency>
    <groupId>com.github.rawls238</groupId>
    <artifactId>Scientist4JCore</artifactId>
    <version>0.7</version>
</dependency>
```
# Usage

This Java port supports most of the functionality of the original Scientist library in Ruby, however its interface is a bit different.

The core component of this library is the `Experiment<T>` class. It's recommended to use this class as a Singleton. The main usage is as follows:

## Basic Usage

You can either run a synchronous experiment or an asynchronous experiment.

For a synchronous experiment, the order in which control and candidate functions are run is randomized.

To run a synchronous experiment:

```java
Experiment<Integer> e = new Experiment("foo");
e.run(this::controlFunction, this::candidateFunction);
```

For an asynchronous experiment, the two functions are run asynchronously.

To run an asynchronous experiment:

```java
Experiment<Integer> e = new Experiment("foo");
e.runAsync(this::controlFunction, this::candidateFunction);
```

Behind the scenes the following occurs in both cases:
* It decides whether or not to run the candidate function
* Measures the durations of all behaviors
* Compares the result of the two
* Swallows (but records) any exceptions raised by the candidate
* Publishes all this information.


## Dropwizard

Out of the box this uses [Dropwizard metrics](https://dropwizard.github.io/metrics/3.1.0/) to report the following stats.
The following metrics are reported which have the form `scientist.[experiment name].*`:

* duration of default behavior in ms
* duration of candidate behavior in ms
* counter of total number of users going through the codepath
* counter of number of mismatches
* counter of candidate exceptions

You can provide your own metric registry object/bean via the constructor or by extending the Experiment class and overriding the `getMetrics` method.

## Optional Configuration

Users can optionally override the following functions:

* publish (to publish results of an experiment if you don't want to use the default Dropwizard metrics)
* compareResults (by default this library just uses `equals` between objects for equality, but in case you want to special case equality between objects)
* enabled (to limit what % of users get exposed to the new code path - by default it's 100%)
* runIf (to enforce conditional behavior on who should be exposed to the new code path)
* isAsync (force using the async for legacy code or move to runAsync method)


License: MIT
