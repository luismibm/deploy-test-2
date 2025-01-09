package com.luismi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Starter(
    @JsonProperty("starter-num") int starterNum,
    @JsonProperty("starter-name") String starterName,
    @JsonProperty("starter-type") String starterType
) {}