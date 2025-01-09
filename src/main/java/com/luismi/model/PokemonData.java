package com.luismi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PokemonData(
    @JsonProperty("pokemon-generations") List<Generation> pokemonGenerations
) {}