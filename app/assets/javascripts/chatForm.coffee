$ ->
    ws = new WebSocket $("#chatForm").data("ws-url")
    $(".start-chat-button").click ->
        receiver = $(this).attr 'id'
        $("#receiver").html receiver
        $("#myForm").show()

        ws.onmessage = (event) ->
            message = event.data
            $("#receiverMsg").html message
        $('#chatForm').submit (submitEvent) -> 
            submitEvent.preventDefault();
            ws.send(JSON.stringify({ receiver, text: $("#myMsg").val()}))
            $("#myMsg").val("")

    $("#closeChat").click ->
        $("#myForm").hide()

