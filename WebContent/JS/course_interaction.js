/*
 * Items Being Used
 *
 * inProgresssNode - the node that represents the task currently being carried out by a user.
 * nodeFrontier - a list of nodes that represent the tasks that may be undertaken next.
 * nodePath - the list that has the taks completed by a user in the order in which they were completed.
 */
var inProgressNode;
var nodeFrontier = [];
var nodePath = [];

/*
 * Prints states of several course interaction variables to the console for debugging
 */
function logVarStates(){
  console.log();
  console.log("currentSelectedItem: " + currentSelectedItem);
  console.log("inProgressNode: " + inProgressNode);
  console.log("currentInProgressGroups: " + currentInProgressGroups);
  console.log("nodeFrontier: " + nodeFrontier);
  console.log("nodePath: " + nodePath);
  console.log();
}

/*
 *  Starts a course by making the first node the inProgress node
 */
function startCourse() {
 resetCourse();
 inProgressNode = getStartNode();
 addInProgressNodeToPath();
 var nodeInDiagram = myDiagram.findNodeForData(inProgressNode);
 addChildrenToFrontier(inProgressNode);
 setAllNodeColors();
}

/*
 * Returns the start node in a diagram
 */
function getStartNode(){
 for(var i = 0; i < myDiagram.model.nodeDataArray.length; i++){
   var curNode = myDiagram.model.nodeDataArray[i];
   if(curNode.nodeType === nodeTypeStartNode
     && curNode.group === undefined){
     return curNode;
   }
 }
}

/*
 * Adds a nodes children to the nodeFrontier by retrieving the passed nodes outgoing connections from
 * the models linkDataArray and adding the end nodes to the frontier.
 */
function addChildrenToFrontier(node){
 var links = myDiagram.model.linkDataArray;
 for(var i = 0; i < links.length; i++){
   var curLink = links[i];
   //if this node is a child of our current node that is not already contained in the frontier
   if(curLink.from === node.key){
       nodeFrontier.push(curLink.to);
   }
 }
}

function oldNextNode(){
 if(inProgressNode !== undefined){
   //mark current node as completed
   makeCompleted(inProgressNode);

   //get next node
   var prevNode = inProgressNode;
   var nextNodeInPath = getNextWorkingNode();

  //here we wait for all incoming connections in an and node to synchronize
   while(((nextNodeInPath.name === nodeNameAND
     || nextNodeInPath.name === nodeTypeSplitAnd
     || nextNodeInPath.name === nodeTypeJoinAnd) &&
     (incomingConnectionsResolved(nextNodeInPath) === false))
     ||
     (nextNodeInPath.name === nodeNameXOR
       && nextNodeInPath.completed === true)
    ){
       nextNodeInPath = getNextWorkingNode();
   }

   inProgressNode = nextNodeInPath;

   //if end node of entire graph has been reached, finish course
   if(inProgressNode.isGroup === undefined
     && inProgressNode.group === undefined
     && inProgressNode.nodeType === nodeTypeEndNode){
     finishCourse();
     setAllNodeColors();
   }

   else {
     //Start subprocesses if node is a subprocess
     if(inProgressNode.isGroup !== undefined){
       if(inProgressNode.isGroup){
         //remove the subprocess node from the frontier and initiate the subprocess
         removeNodeFromFrontier(inProgressNode);
         inProgressNode = startSubProcess(inProgressNode.key);
       }
     }

     else if( inProgressNode.group !== undefined
      && inProgressNode.nodeType === nodeTypeEndNode){
        finishSubProcess();
     }

    else if( inProgressNode.name.substring(0, 6) === nodeNameSignal){
      console.log("eventTrigger");
      triggerEvent();
    }

     //if the previous node is an xor node we should remove its outgoing
     //connections from the frontier  except for the chosen node
     if(prevNode.name === nodeNameXOR){
       removeXOROutgoingConnectionsFromFrontier(prevNode.key, inProgressNode.key);
     }


     removeNodeFromFrontier(inProgressNode);
     addChildrenToFrontier(inProgressNode);
     setAllNodeColors();

     nodePath.push(inProgressNode.key);

     if(inProgressNode.name === nodeNameAND){
       nextNode();
     }
   }
 }
}

function nextNode(){
  if(nodeFrontier.length === 1){
    advanceToNode(getNextWorkingNode());
  }
  else if(hasOneNodeInFrontier() === true){
    advanceToNode(getNextWorkingNode());
  }
}

function hasOneNodeInFrontier(){
  var curKey = nodeFrontier[0];
  for(var i = 0; i < nodeFrontier.length; i ++){
    if(nodeFrontier[i] !== curKey){
      return false;
    }
  }
  return true;
}

function advanceToNode(nextNodeInPath){

   var validNextNode = isValidMove(nextNodeInPath);

   if(validNextNode === true){
     //mark current node as completed
     makeCompleted(inProgressNode);

     //get next node
     var prevNode = inProgressNode;

     inProgressNode = nextNodeInPath;

     //if end node of entire graph has been reached, finish course
     if(inProgressNode.isGroup === undefined
       && inProgressNode.group === undefined
       && inProgressNode.nodeType === nodeTypeEndNode){
       finishCourse();
       setAllNodeColors();
     }

     else {
       //Start subprocesses if node is a subprocess
       if(inProgressNode.isGroup !== undefined){
         if(inProgressNode.isGroup){
           //remove the subprocess node from the frontier and initiate the subprocess
           removeNodeFromFrontier(inProgressNode);
           inProgressNode = startSubProcess(inProgressNode.key);
         }
       }

       else if( inProgressNode.group !== undefined
        && inProgressNode.nodeType === nodeTypeEndNode){
          finishSubProcess();
       }

      else if( inProgressNode.name.substring(0, 6) === nodeNameSignal){
        console.log("eventTrigger");
        triggerEvent();
      }

       //if the previous node is an xor node we should remove its outgoing
       //connections from the frontier  except for the chosen node
       if(prevNode.name === nodeNameXOR){
         removeXOROutgoingConnectionsFromFrontier(prevNode.key, inProgressNode.key);
       }


       removeNodeFromFrontier(inProgressNode);
       addChildrenToFrontier(inProgressNode);
       setAllNodeColors();

       nodePath.push(inProgressNode.key);

       if(inProgressNode.name === nodeNameAND){
         nextNode();
       }
     }
  }
}

function isValidMove(node){
  //here we wait for all incoming connections in an and node to synchronize and
  //check that our node is not an option in a completed XOR
  if(
    ((node.name === nodeNameAND
    || node.name === nodeTypeSplitAnd
    || node.name === nodeTypeJoinAnd) &&
    (incomingConnectionsResolved(node) === false))
    ||
    (node.name === nodeNameXOR
      && node.completed === true)
   ){
      return false;
  }
  return true;
}

function incomingConnectionsResolved(node){
  var incomingConnections = getIncomingConnections(node);
  if(contains(nodePath, incomingConnections) === true){
    return true;
  }
  else{
    return false;
  }
}

function getIncomingConnections(node){
  var incomingConnections = [];
  for(var i = 0; i < currentProcess.connections.length; i ++){
    if(currentProcess.connections[i].to === node.key){
      incomingConnections.push(currentProcess.connections[i].from);
    }
  }
  return incomingConnections
}


function contains(arr1, arr2){
  for(var i = 0; i < arr2.length; i ++){
    if(arr1.indexOf(arr2[i]) === -1)
      return false;
  }
  return true;
}

function makeCompleted(node){
 var index = node.key;
 for(var i = 0; i < myDiagram.model.nodeDataArray.length; i ++){
   if(myDiagram.model.nodeDataArray[i].key == node.key){
     myDiagram.model.nodeDataArray[i].completed = true;
   }
 }
}

function getNextWorkingNode(){
 var index = Math.floor(Math.random() * nodeFrontier.length);
 for(var i = 0; i < myDiagram.model.nodeDataArray.length; i ++){
   if(nodeFrontier[index] === myDiagram.model.nodeDataArray[i].key){
     return myDiagram.model.nodeDataArray[i];;
   }
 }
}

function removeNodeFromFrontier(node){
 //-1 is the return value when item is not present in array
 while(nodeFrontier.indexOf(node.key) !== -1){
   nodeFrontier.splice(nodeFrontier.indexOf(node.key), 1);
 }
}

function prevNode(){
  if( inProgressNode !== undefined){
    if (!(inProgressNode.nodeType === nodeTypeStartNode && inProgressNode.group === undefined)){
      inProgressNode.completed = false;
      removeOutgoingConnectionsFromFrontier(inProgressNode);
      nodePath.pop();
      //moveBackToNode(getNodeByKey(nodePath[nodePath.length - 1]));
      inProgressNode = getNodeByKey(nodePath[nodePath.length - 1]);
      removeOutgoingConnectionsFromFrontier(inProgressNode);
      addChildrenToFrontier(inProgressNode);
      setAllNodeColors();
    }
  }
}

//resets the progress data for a course.
function resetCourse(){
 inProgressNode = undefined;
 currentInProgressGroups = [];
 nodeFrontier = [];
 nodePath = [];

 for(var i = 0; i < myDiagram.model.nodeDataArray.length; i ++){
   myDiagram.model.nodeDataArray[i].completed = false;
   var nodeInDiagram = myDiagram.findNodeForData(myDiagram.model.nodeDataArray[i]);
   setItemColor(nodeInDiagram, false);
 }
}

function finishCourse(){
 addInProgressNodeToPath();
 makeCompleted(inProgressNode);
 inProgressNode = undefined;
 currentInProgressGroups = [];
 nodeFrontier = [];
}

function finishSubProcess(){

  var numDigitsInGroupKey = 1 + (inProgressNode.group / 10);
  for(var i = 0; i < nodeFrontier[i]; i ++){
    if(isNaN(nodeFrontier[i])){
      if(nodeFrontier[i].substring(0, numDigitsInGroupKey) === (inProgressNode.group + "-")){
        nodeFrontier.splice(nodeFrontier.indexOf(nodeFrontier[i]), 1);
      }
    }
  }
  addChildrenToFrontier(getNodeByKey(inProgressNode.group));
  nodePath.push(inProgressNode.group);

}

function removeXOROutgoingConnectionsFromFrontier(xorNodeKey, currentNodeKey){
  var outGoingConnections = getOutGoingConnections(xorNodeKey);
  outGoingConnections.splice(outGoingConnections.indexOf(currentNodeKey), 1);

  for(var i = 0; i < outGoingConnections.length; i ++){
    nodeFrontier.splice(nodeFrontier.indexOf(outGoingConnections[i]), 1);
  }
}

function removeOutgoingConnectionsFromFrontier(node){
  var outGoingConnections = getOutGoingConnections(node.key);

  for(var i = 0; i < outGoingConnections.length; i ++){
    nodeFrontier.splice(nodeFrontier.indexOf(outGoingConnections[i]), 1);
  }
}

function getOutGoingConnections(nodeKey){
  var outGoingConnections = [];
  for(var i = 0; i < currentProcess.connections.length; i ++){
    if(currentProcess.connections[i].from === nodeKey){
      outGoingConnections.push(currentProcess.connections[i].to);
    }
  }
  return outGoingConnections;
}

function addInProgressNodeToPath(){
  nodePath.push(inProgressNode.key);
}

function triggerEvent(){
  makeCompleted(inProgressNode);
  removeNodeFromFrontier(inProgressNode);

  //extract name of event to trigger
  var eventName = inProgressNode.name.substring(7);
  console.log(eventName);
  //add in progress node to path
  addInProgressNodeToPath();
  //get new event node
  //and set new event to be the inprogress node
  for(var i = 0; i < currentProcess.nodeData.length; i++){
    if(currentProcess.nodeData[i].name === eventName){
      inProgressNode = currentProcess.nodeData[i];
    }
  }
}

function startSubProcess(groupKey){
  for(var i = 0; i < currentProcess.nodeData.length; i ++){
    if(currentProcess.nodeData[i].group === groupKey
      && currentProcess.nodeData[i].nodeType === nodeTypeStartNode)
      return currentProcess.nodeData[i];
  }
}
