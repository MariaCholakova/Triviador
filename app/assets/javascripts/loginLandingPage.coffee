$ ->
    ws = new WebSocket $("#chatForm").data("ws-url")
    ws.onmessage = (event) ->
        message = event.data
        $("#receivedMsg").html message
    $('#chatForm').submit (submitEvent) -> 
        submitEvent.preventDefault();
        ws.send($("#myMsg").val())

    $("#openChat").click ->
        console.log("show")
        $("#myForm").show()

    $("#closeChat").click ->
        $("#myForm").hide()


