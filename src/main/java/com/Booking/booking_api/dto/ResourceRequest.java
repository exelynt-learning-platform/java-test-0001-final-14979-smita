package com.Booking.booking_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceRequest {

    @NotBlank(message = "Resource name is required")
    @Size(max = 100, message = "Resource name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Resource type is required")
    @Size(max = 50, message = "Resource type must not exceed 50 characters")
    private String type;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean available = true;

    public ResourceRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}
