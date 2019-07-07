$ ->
    ws = new WebSocket $("#chatForm").data("ws-url")
    ws.onmessage = (event) ->
        message = event.data
        $("#receivedMsg").html message
    $('#chatForm').submit (submitEvent) -> 
        submitEvent.preventDefault();
        ws.send($("#myMsg").val())

