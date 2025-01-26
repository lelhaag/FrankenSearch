# FrankenSearch

FrankenSearch is a research project leveraging the **Ludii framework** for AI-driven experiments and evolutionary algorithm development. This repository contains code for search algorithms, evolutionary experiments, and game configurations.

This project is part of my master's thesis, which can be found in `FrankenSearch.pdf` in this repository.

## Repository Structure

```
resources/
src/
├── algos/       # Search algorithm implementations
├── evolution/   # Evolutionary algorithm and utilities
├── main/        # Launchers for experiments
├── parser/      # Parsing and compiling logic
├── utils/       # Supporting utilities and helpers
```

### Key Folders
- **`resources/`**: Contains Ludii `.lud` files and SADL descriptions.
- **`src/evolution/`**: Main entry point for evolutionary experiments.
- **`src/main/`**: Experiment launchers for different configurations.

## Prerequisites

- **Java 8+**
- **Ludii Framework**: Download from [Ludii's official website](https://ludiigames.org/).

To integrate Ludii, download the jar file and place it in the `resources/` folder. Update your classpath to include the jar file when compiling and running.

## How to Run

### Running Experiments
Navigate to `src/main/` and choose a launcher file (e.g., `LaunchLudiiExp.java`). Other experiments can be triggered with appropriate configuration from the files in `main`. Note that `LaunchLudii.java` starts the Ludii GUI, whereas the others run without GUI.

### Running Evolution
Configure parameters in `src/evolution/EvolutionaryAlgorithm.java` and run the file.

## Resources
- **Ludii Example AI Repository**: Documentation and examples for Ludii integration. [Ludii Example AI Repository](https://github.com/Ludeme/LudiiExampleAI)