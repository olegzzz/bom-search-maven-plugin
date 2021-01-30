# BOM search maven plugin

![Master Status](https://github.com/olegzzz/bom-search-maven-plugin/workflows/maven-ci/badge.svg)
![CodeQL](https://github.com/olegzzz/bom-search-maven-plugin/workflows/CodeQL/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.olegzzz/bom-search-maven-plugin)](https://maven-badges.herokuapp.com/maven-central/com.github.olegzzz/bom-search-maven-plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)


Plugin to lookup bill of materials (BOM) artifacts to retrofit existing project.

## Motivation

Oftentimes a project of decent size contains multiple dependencies that share the same group. More often than not those 
dependencies managed via a common version hardcoded into project properties. Sometimes dependency management is used. 
In any case, it would greatly simplify dependency management if one single BOM file can be referenced to make sure all 
related dependencies have specific versions. 

Here is a problem: with a lot of dependencies in the pom-file it's hard to figure out if any two or more share same 
group or if BOM available for those groups. 

This plugin tries to do just that: given the project pom to lookup possible BOM artifacts for a group of dependencies.

## Goals

`search` runs a search for available BOM artifacts for current project.

`enforce` fails the build if it finds BOM artifacts available for current project but not used.

### Configuration

Name | Type | Description 
----------|------|---------
`<minOccurrence>` | int | Minimal number of dependencies that share a group to search for BOM for that group. <br/>**User property**: `bomsearch.minOccurrence`<br/>**Default value**: `2`
`<mavenRepoUrl>` | URL | Maven repository URL to search artifact. <br/>**User property**: `bomsearch.mavenRepoUrl`<br/>**Default value**: `https://repo.maven.apache.org/maven2`
`<incremental>` | boolean | Whether to use previously stored results. <br/>**User property**: `bomsearch.incremental` <br/>**Default value**: `true`
`<lenient>` | boolean | If set to `false`, `enforce` goal will not fail the build, but still logs warnings. <br/>**User property**: `bomsearch.lenient` <br/>**Default value**: `false`
`<skip>` | boolean | Disable plugin <br/>**User property**: `bomsearch.skip` <br/>**Default value**: `false`

### Usage

Add to `<build>` section of the pom-file:

```
<plugin>
    <groupId>com.github.olegzzz</groupId>
    <artifactId>bom-search-maven-plugin</artifactId>
    <version>1.5</version>
    <executions>
        <execution>
            <id>default-cli</id>
            <goals>
                <goal>enforce</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
and run `mvn verify`

Build log will contain warnings with suggested BOM dependencies to include into the project. Let's say a project has multiple dropwizard dependencies, then looking into the log one can pick suggested BOM file (`io.dropwizard:dropwizard-bom` in this case):

```
[INFO] --- bom-search-maven-plugin:1.5:enforce (default-cli) ---
[INFO] Following BOMs found for module: [io.dropwizard:dropwizard-bom]
[WARNING] Following BOMs available but not used: [io.dropwizard:dropwizard-bom]
```
