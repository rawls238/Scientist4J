# Scientist4J

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.rawls238/Scientist4J/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.rawls238/Scientist4J)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.github.rawls238/Scientist4J/badge.svg)](http://www.javadoc.io/doc/com.github.rawls238/Scientist4J)

A port of Github's refactoring tool [Scientist](https://github.com/github/scientist) in Java

# Installation

```java
<dependency>
    <groupId>com.github.rawls238</groupId>
    <artifactId>Scientist4J</artifactId>
    <version>0.1</version>
</dependency>
```
# Usage

This Java port supports most of the functionality of the original Scientist library in Ruby, however its interface is a bit different.

The core component of this library is the `Experiment<T>` class. It's recommended to use this class as a Singleton. The main usage is as follows:

```java
Experiment<Integer> e = new Experiment("foo");
e.run(this::controlFunction, this::candidateFunction);
```

This does a bunch of stuff behind the scenes:
* It decides whether or not to run the candidate function,
* Randomizes the order in which control and candidate functions are run,
* Measures the durations of all behaviors,
* Compares the result of the two,
* Swallows (but records) any exceptions raised by the candidate and
* Publishes all this information.

Out of the box this uses [Dropwizard metrics](https://dropwizard.github.io/metrics/3.1.0/) to report the following stats.
The following metrics are reported which have the form `scientist.[experiment name].*`:

* duration of default behavior in ms
* duration of candidate behavior in ms
* counter of total number of users going through the codepath
* counter of number of mismatches


Users can optionally override the following functions:

* publish (to publish results of an experiment if you don't want to use the default Dropwizard metrics)
* compareResults (by default this library just uses `equals` between objects for equality, but in case you want to special case equality between objects)
* enabled (to limit what % of users get exposed to the new code path - by default it's 100%)
* runIf (to enforce conditional behavior on who should be exposed to the new code path)


License: MIT
