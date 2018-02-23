package org.wso2.broker.auth.rest.model;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ActionUserGroupsMapping   {
  
  private @Valid String action = null;
  private @Valid List<String> userGroups = new ArrayList<String>();

  /**
   * Resource action
   **/
  public ActionUserGroupsMapping action(String action) {
    this.action = action;
    return this;
  }

  
  @ApiModelProperty(value = "Resource action")
  @JsonProperty("action")
  public String getAction() {
    return action;
  }
  public void setAction(String action) {
    this.action = action;
  }

  /**
   * Set of user groups for a scope
   **/
  public ActionUserGroupsMapping userGroups(List<String> userGroups) {
    this.userGroups = userGroups;
    return this;
  }

  
  @ApiModelProperty(value = "Set of user groups for a scope")
  @JsonProperty("userGroups")
  public List<String> getUserGroups() {
    return userGroups;
  }
  public void setUserGroups(List<String> userGroups) {
    this.userGroups = userGroups;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActionUserGroupsMapping actionUserGroupsMapping = (ActionUserGroupsMapping) o;
    return Objects.equals(action, actionUserGroupsMapping.action) &&
        Objects.equals(userGroups, actionUserGroupsMapping.userGroups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, userGroups);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActionUserGroupsMapping {\n");
    
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
    sb.append("    userGroups: ").append(toIndentedString(userGroups)).append("\n");
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

