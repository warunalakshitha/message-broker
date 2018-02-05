package org.wso2.broker.auth.rest.model;

import java.util.ArrayList;
import java.util.List;
import org.wso2.broker.auth.rest.model.ActionUserGroupsMapping;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class QueueAuthData   {
  
  private @Valid String name = null;
  private @Valid String owner = null;
  private @Valid Boolean durable = null;
  private @Valid List<ActionUserGroupsMapping> mappings = new ArrayList<ActionUserGroupsMapping>();

  /**
   * Queue name
   **/
  public QueueAuthData name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Queue name")
  @JsonProperty("name")
  @NotNull
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Queue owner
   **/
  public QueueAuthData owner(String owner) {
    this.owner = owner;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Queue owner")
  @JsonProperty("owner")
  @NotNull
  public String getOwner() {
    return owner;
  }
  public void setOwner(String owner) {
    this.owner = owner;
  }

  /**
   * Queue is durable or not
   **/
  public QueueAuthData durable(Boolean durable) {
    this.durable = durable;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Queue is durable or not")
  @JsonProperty("durable")
  @NotNull
  public Boolean isDurable() {
    return durable;
  }
  public void setDurable(Boolean durable) {
    this.durable = durable;
  }

  /**
   * Queue action User groups mappings
   **/
  public QueueAuthData mappings(List<ActionUserGroupsMapping> mappings) {
    this.mappings = mappings;
    return this;
  }

  
  @ApiModelProperty(value = "Queue action User groups mappings")
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
    QueueAuthData queueAuthData = (QueueAuthData) o;
    return Objects.equals(name, queueAuthData.name) &&
        Objects.equals(owner, queueAuthData.owner) &&
        Objects.equals(durable, queueAuthData.durable) &&
        Objects.equals(mappings, queueAuthData.mappings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, owner, durable, mappings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QueueAuthData {\n");
    
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

