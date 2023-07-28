# TornadoFX v2

A JavaFX framework for Kotlin (Java 11+)

Current state: [![Build Status](https://travis-ci.com/confinitum/tornadofx2.svg)](https://travis-ci.com/confinitum/tornadofx2)

## Features

* Supports both MVC, MVP and their derivatives
* Dependency injection
* Type safe GUI builders
* Type safe CSS builders
* First class FXML support
* Async task execution
* EventBus with thread targeting
* Hot reload of Views and Stylesheets
* Zero config, no XML, no annotations

## Requirements

* [Kotlin 1.4+](https://kotlinlang.org/)
* [JavaFX 15+](https://openjfx.io/)

## Installation

In repositories block add

```
maven {
    url = uri("https://oss.sonatype.org/content/repositories/snapshots")
}
```

In dependencies block add
```
implementation("no.tornado:tornadofx:2.0.0-SNAPSHOT")
```

## Feature Requests and Bugs

Please use the GitHub [issues](https://github.com/edvin/tornadofx2/issues) for all feature requests and bugs.

## Maintainers

* [Edvin Syse](https://github.com/edvin)
* [GoToTags](https://gototags.com/) / [Craig Tadlock](https://www.linkedin.com/in/ctadlock/)

## Developer Info

* Kotlin & Java target is Java 11
* JUnit5 plus vintage engine for unit testing
* Tests running on classpath ignoring javas module system
* no support for OSGI bundle

