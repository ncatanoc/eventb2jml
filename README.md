# EventB2JML

EventB2JML is a code-generation tool that translates Event-B models into JML (Java Modeling Language) specifications. The tool enables the use of Event-B formal models as a basis for the specification and verification of Java software.

EventB2JML was developed to bridge the gap between formal modelling and software verification by automatically generating JML contracts from Event-B machines. The generated specifications can be used with JML-based verification and runtime checking tools.

## Main Contributions

- Automatic translation of Event-B specifications into JML.
- Preservation of Event-B invariants as JML class invariants.
- Translation of Event-B events into JML method specifications.
- Support for the verification of Java implementations against formally verified Event-B models.
- Integration of formal modelling and design-by-contract techniques.

## Verification Approach

EventB2JML generates JML specifications from Event-B models, allowing Java programs to be verified against requirements expressed and analysed at the formal modelling level.

The generated JML contracts capture:

- State invariants
- Preconditions
- Postconditions
- Frame conditions

This approach facilitates traceability from formal requirements to software implementations.

## Applicability

EventB2JML can be used in:

- Formal software development
- Safety-critical systems
- Security-critical systems
- Design-by-contract verification
- Model-driven software engineering

## Research Prototype Status

This repository contains an archived academic research prototype released for reproducibility, reference, and educational purposes.

## Related Publication

N. Cataño and R. Rivera.

**EventB2JML: Specification Generation from Event-B Models**

Springer, 2013.

DOI: https://doi.org/10.1007/978-3-319-40648-0_13
