# Jakarta WebSocket TCK Compliance Module

This module is dedicated to replicate the official Jakarta WebSocket 2.2 TCK (Technology Compatibility Kit) against the OSGi WebSocket implementation.

## Overview

The Jakarta WebSocket TCK is a comprehensive test suite designed to verify that implementations comply with the [Jakarta WebSocket 2.2 Specification](https://jakarta.ee/specifications/websocket/2.2/). Running the TCK against our implementation ensures compatibility and correctness.

## Goals

- The original TCK and its sources can be found in /websocket-tck
- The TCK is designed for Jakarta EE environments, so adaptation for OSGi may require creative solutions
- Not all tests may be applicable to our use case (e.g., CDI integration tests)
- Some tests may be excluded if they test optional features not implemented
- The goal is maximum compliance while maintaining our architecture

## Current status

Not implemented, plan is needed for:

- get an overview of testcase contained in the TCK sources
- match them up with the specification https://jakarta.ee/specifications/websocket/2.2/jakarta-websocket-spec-2.2 to see whats covered
- create a matrix of current supported feature in our implementation
- form a plan for migrating the testcase in an equivalent way so they are suitable for our implementation, we do NOT want to reuse them as-is as the setup is to complex and some are maybe not applicable
- update the copilot-instructions.md file with proper instructions for future session that it extracts relevant test cases and port them to our new websocket compliance test and always also update this readme here