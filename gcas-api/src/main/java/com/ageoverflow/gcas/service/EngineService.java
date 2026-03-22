package com.ageoverflow.gcas.service;

import com.ageoverflow.gcas.exception.EngineException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Thin wrapper around the AgeOverflow-Engine command-line binary.
 * Serialises input to JSON, writes it to the process stdin, and deserialises the result from stdout.
 * Throws {@link com.ageoverflow.gcas.exception.EngineException} on any process or parse failure.
 */
@Service
public class EngineService {

    private final ObjectMapper objectMapper;
    private final String enginePath;

    public EngineService(ObjectMapper objectMapper, @Value("${engine.path}") String enginePath) {
        this.objectMapper = objectMapper;
        this.enginePath = enginePath;
    }

    /**
     * Runs the engine binary synchronously for the given request and photo set.
     * Passes a JSON payload via stdin and reads the result JSON from stdout.
     *
     * @param requestId used as the {@code id} field in the engine's input payload
     * @param photos    base64-encoded photo strings to analyse
     * @return the parsed {@link EngineResult} from the engine's stdout
     * @throws com.ageoverflow.gcas.exception.EngineException if the process fails to start,
     *         exits with a non-zero code, or returns unparseable output
     */
    public EngineResult compute(UUID requestId, List<String> photos) {
        String inputJson;
        try {
            inputJson = objectMapper.writeValueAsString(new EngineInput(requestId.toString(), photos));
        } catch (IOException e) {
            throw new EngineException("Failed to serialise engine input", e);
        }

        Process process;
        try {
            process = new ProcessBuilder(enginePath, "compute", "--output", "-")
                    .redirectErrorStream(false)
                    .start();
        } catch (IOException e) {
            throw new EngineException("Failed to start engine process", e);
        }

        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(inputJson.getBytes());
        } catch (IOException e) {
            throw new EngineException("Failed to write to engine stdin", e);
        }

        String output;
        try {
            output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new EngineException("Engine exited with code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new EngineException("Failed to read engine output", e);
        }

        try {
            EngineOutput engineOutput = objectMapper.readValue(output, EngineOutput.class);
            return engineOutput.results();
        } catch (IOException e) {
            throw new EngineException("Failed to parse engine output: " + output, e);
        }
    }

    // Internal JSON mapping classes

    private record EngineInput(String id, List<String> content) {}

    public record EngineResult(
            String checksum,
            EngineGenerations generations,
            @JsonProperty("primary_generation") String primaryGeneration,
            double age
    ) {}

    public record EngineGenerations(
            double silent,
            @JsonProperty("baby_boomers") double babyBoomers,
            double x,
            double y,
            double z,
            double alpha
    ) {}

    private record EngineOutput(String id, EngineResult results) {}
}
