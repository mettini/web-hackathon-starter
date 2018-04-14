$(document).ready(function() {
  $(document).on("click", ".confirm-link", function(event) {
    event.preventDefault();
    if (confirm($(this).data("confirm-text"))) location.href = $(this).attr("href");
  });
});
