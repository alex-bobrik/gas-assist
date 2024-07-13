document.addEventListener('DOMContentLoaded', () => {
    processCheckLinethrough();
});

// Strikethrough text scripts
function processCheckLinethrough() {
    document.querySelectorAll('.can-cross').forEach(function(checkbox) {
        checkbox.addEventListener('change', function() {
            var label = document.querySelector('label[for="' + this.id + '"]');
            if (this.checked) {
                label.classList.add('crossed');
            } else {
                label.classList.remove('crossed');
            }
        });
    });
}

// Timer scripts
function startTimer(timeMins, timerName) {
    AndroidInterface.startTimer(timeMins, timerName);
}

// Image zoom scripts
document.getElementById('file-input').addEventListener('change', function(event) {
    const file = event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const img = document.createElement('img');
            img.src = e.target.result;
            img.addEventListener('click', function() {
                const modal = document.getElementById('modal');
                const modalImage = document.getElementById('modal-image');
                modalImage.src = e.target.result;
                modal.style.display = 'flex';
            });
            const imageContainer = document.getElementById('image-container');
            imageContainer.innerHTML = '';
            imageContainer.appendChild(img);
        }
        reader.readAsDataURL(file);
    }
});

document.querySelector('#modal .close').addEventListener('click', function() {
    document.getElementById('modal').style.display = 'none';
});

document.getElementById('modal').addEventListener('click', function(event) {
    if (event.target === this) {
        this.style.display = 'none';
    }
});

// move by fingers
const modalImage = document.getElementById('modal-image');
let scale = 1;
let startX = 0;
let startY = 0;
let initialDistance = 0;
let offsetX = 0;
let offsetY = 0;

modalImage.addEventListener('touchstart', function(event) {
    if (event.touches.length === 2) {
        initialDistance = getDistance(event.touches[0], event.touches[1]);
    } else if (event.touches.length === 1) {
        startX = event.touches[0].clientX - offsetX;
        startY = event.touches[0].clientY - offsetY;
    }
});

modalImage.addEventListener('touchmove', function(event) {
    if (event.touches.length === 2) {
        event.preventDefault();
        const currentDistance = getDistance(event.touches[0], event.touches[1]);
        scale *= currentDistance / initialDistance;
        initialDistance = currentDistance;
        modalImage.style.transform = `scale(${scale}) translate(${offsetX}px, ${offsetY}px)`;
    } else if (event.touches.length === 1) {
        event.preventDefault();
        offsetX = event.touches[0].clientX - startX;
        offsetY = event.touches[0].clientY - startY;
        modalImage.style.transform = `scale(${scale}) translate(${offsetX}px, ${offsetY}px)`;
    }
});

function getDistance(touch1, touch2) {
    const dx = touch2.clientX - touch1.clientX;
    const dy = touch2.clientY - touch1.clientY;
    return Math.sqrt(dx * dx + dy * dy);
}
