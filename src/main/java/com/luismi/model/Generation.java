package com.luismi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Generation(
    @JsonProperty("generation-num") int generationNum,
    @JsonProperty("generation-name") String generationName,
    @JsonProperty("generation-map") String generationMap,
    @JsonProperty("starters") List<Starter> starters
) {}