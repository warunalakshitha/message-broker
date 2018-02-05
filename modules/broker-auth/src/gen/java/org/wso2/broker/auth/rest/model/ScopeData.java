package org.wso2.broker.auth.rest.model;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ScopeData   {
  
  private @Valid String name = null;
  private @Valid List<String> authoriedUserGroups = new ArrayList<String>();

  /**
   * Scope name
   **/
  public ScopeData name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Scope name")
  @JsonProperty("name")
  @NotNull
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * User groups of the scope
   **/
  public ScopeData authoriedUserGroups(List<String> authoriedUserGroups) {
    this.authoriedUserGroups = authoriedUserGroups;
    return this;
  }

  
  @ApiModelProperty(value = "User groups of the scope")
  @JsonProperty("authoriedUserGroups")
  public List<String> getAuthoriedUserGroups() {
    return authoriedUserGroups;
  }
  public void setAuthoriedUserGroups(List<String> authoriedUserGroups) {
    this.authoriedUserGroups = authoriedUserGroups;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScopeData scopeData = (ScopeData) o;
    return Objects.equals(name, scopeData.name) &&
        Objects.equals(authoriedUserGroups, scopeData.authoriedUserGroups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, authoriedUserGroups);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScopeData {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    authoriedUserGroups: ").append(toIndentedString(authoriedUserGroups)).append("\n");
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

