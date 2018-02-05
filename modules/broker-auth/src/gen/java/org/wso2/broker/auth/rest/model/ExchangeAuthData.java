package org.wso2.broker.auth.rest.model;

import java.util.ArrayList;
import java.util.List;
import org.wso2.broker.auth.rest.model.ActionUserGroupsMapping;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ExchangeAuthData   {
  
  private @Valid String name = null;
  private @Valid String owner = null;
  private @Valid Boolean durable = null;
  private @Valid List<ActionUserGroupsMapping> mappings = new ArrayList<ActionUserGroupsMapping>();

  /**
   * Exchange name
   **/
  public ExchangeAuthData name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Exchange name")
  @JsonProperty("name")
  @NotNull
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Exchange owner
   **/
  public ExchangeAuthData owner(String owner) {
    this.owner = owner;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Exchange owner")
  @JsonProperty("owner")
  @NotNull
  public String getOwner() {
    return owner;
  }
  public void setOwner(String owner) {
    this.owner = owner;
  }

  /**
   * Exchange is durable or not
   **/
  public ExchangeAuthData durable(Boolean durable) {
    this.durable = durable;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Exchange is durable or not")
  @JsonProperty("durable")
  @NotNull
  public Boolean isDurable() {
    return durable;
  }
  public void setDurable(Boolean durable) {
    this.durable = durable;
  }

  /**
   * Exchange action User groups mappings
   **/
  public ExchangeAuthData mappings(List<ActionUserGroupsMapping> mappings) {
    this.mappings = mappings;
    return this;
  }

  
  @ApiModelProperty(value = "Exchange action User groups mappings")
  @JsonProperty("mappings")
  public List<ActionUserGroupsMapping> getMappings() {
    return mappings;
  }
  public void setMappings(List<ActionUserGroupsMapping> mappings) {
    this.mappings = mappings;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExchangeAuthData exchangeAuthData = (ExchangeAuthData) o;
    return Objects.equals(name, exchangeAuthData.name) &&
        Objects.equals(owner, exchangeAuthData.owner) &&
        Objects.equals(durable, exchangeAuthData.durable) &&
        Objects.equals(mappings, exchangeAuthData.mappings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, owner, durable, mappings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExchangeAuthData {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    durable: ").append(toIndentedString(durable)).append("\n");
    sb.append("    mappings: ").append(toIndentedString(mappings)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

