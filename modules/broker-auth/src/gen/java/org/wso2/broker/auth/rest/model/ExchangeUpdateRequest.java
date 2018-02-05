package org.wso2.broker.auth.rest.model;

import java.util.ArrayList;
import java.util.List;
import org.wso2.broker.auth.rest.model.ActionUserGroupsMapping;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ExchangeUpdateRequest   {
  
  private @Valid String owner = null;
  private @Valid Boolean durable = null;
  private @Valid List<ActionUserGroupsMapping> authorizedUserGroups = new ArrayList<ActionUserGroupsMapping>();

  /**
   * Owner of exchange
   **/
  public ExchangeUpdateRequest owner(String owner) {
    this.owner = owner;
    return this;
  }

  
  @ApiModelProperty(value = "Owner of exchange")
  @JsonProperty("owner")
  public String getOwner() {
    return owner;
  }
  public void setOwner(String owner) {
    this.owner = owner;
  }

  /**
   * Is exchange durable
   **/
  public ExchangeUpdateRequest durable(Boolean durable) {
    this.durable = durable;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Is exchange durable")
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
  public ExchangeUpdateRequest authorizedUserGroups(List<ActionUserGroupsMapping> authorizedUserGroups) {
    this.authorizedUserGroups = authorizedUserGroups;
    return this;
  }

  
  @ApiModelProperty(value = "Exchange action User groups mappings")
  @JsonProperty("authorizedUserGroups")
  public List<ActionUserGroupsMapping> getAuthorizedUserGroups() {
    return authorizedUserGroups;
  }
  public void setAuthorizedUserGroups(List<ActionUserGroupsMapping> authorizedUserGroups) {
    this.authorizedUserGroups = authorizedUserGroups;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExchangeUpdateRequest exchangeUpdateRequest = (ExchangeUpdateRequest) o;
    return Objects.equals(owner, exchangeUpdateRequest.owner) &&
        Objects.equals(durable, exchangeUpdateRequest.durable) &&
        Objects.equals(authorizedUserGroups, exchangeUpdateRequest.authorizedUserGroups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(owner, durable, authorizedUserGroups);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExchangeUpdateRequest {\n");
    
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    durable: ").append(toIndentedString(durable)).append("\n");
    sb.append("    authorizedUserGroups: ").append(toIndentedString(authorizedUserGroups)).append("\n");
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

