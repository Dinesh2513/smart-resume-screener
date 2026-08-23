const form = document.getElementById('screening-form');
const fileInput = document.getElementById('resumes');
const fileSummary = document.getElementById('file-summary');
const candidateList = document.getElementById('candidate-list');
const emptyState = document.getElementById('empty-state');
const alertBox = document.getElementById('alert');
const loadingOverlay = document.getElementById('loading-overlay');
const screenButton = document.getElementById('screen-button');

fileInput.addEventListener('change', () => {
    const count = fileInput.files.length;
    fileSummary.textContent = count === 0
        ? 'No files selected'
        : `${count} resume${count === 1 ? '' : 's'} selected`;
});

document.getElementById('refresh-button').addEventListener('click', loadCandidates);

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearAlert();

    if (fileInput.files.length > 20) {
        showAlert('Select a maximum of 20 resumes.', 'error');
        return;
    }

    const formData = new FormData();
    formData.append('jobTitle', document.getElementById('job-title').value.trim());
    formData.append('jobDescription', document.getElementById('job-description').value.trim());
    Array.from(fileInput.files).forEach(file => formData.append('resumes', file));

    setLoading(true);
    try {
        const response = await fetch('/api/candidates/screen', {
            method: 'POST',
            body: formData
        });
        const payload = await readPayload(response);
        if (!response.ok) {
            throw new Error(payload.message || 'Screening failed.');
        }
        showAlert(
            `${payload.length} candidate${payload.length === 1 ? '' : 's'} analysed successfully.`,
            'success'
        );
        await loadCandidates();
    } catch (error) {
        showAlert(error.message, 'error');
    } finally {
        setLoading(false);
    }
});

async function loadCandidates() {
    try {
        const response = await fetch('/api/candidates/ranked');
        const payload = await readPayload(response);
        if (!response.ok) {
            throw new Error(payload.message || 'Could not load candidates.');
        }
        renderCandidates(payload);
    } catch (error) {
        showAlert(error.message, 'error');
    }
}

function renderCandidates(candidates) {
    candidateList.innerHTML = '';
    emptyState.style.display = candidates.length ? 'none' : 'grid';

    const scored = candidates.filter(candidate => candidate.matchScore !== null);
    const shortlisted = candidates.filter(candidate => candidate.status === 'SHORTLISTED');
    const average = scored.length
        ? (scored.reduce((sum, candidate) => sum + candidate.matchScore, 0) / scored.length).toFixed(1)
        : '—';

    document.getElementById('total-count').textContent = candidates.length;
    document.getElementById('shortlisted-count').textContent = shortlisted.length;
    document.getElementById('average-score').textContent = average === '—' ? average : `${average}/10`;

    candidates.forEach((candidate, index) => {
        const score = candidate.matchScore ?? '—';
        const scoreClass = candidate.matchScore >= 7
            ? 'high'
            : candidate.matchScore !== null && candidate.matchScore < 5 ? 'low' : '';
        const statusClass = candidate.status.toLowerCase().replaceAll('_', '-');
        const matched = candidate.matchedSkills || [];
        const missing = candidate.missingSkills || [];
        const improvements = candidate.improvementRecommendations || [];
        const previewSkills = matched.length ? matched : candidate.skills || [];

        const card = document.createElement('article');
        card.className = 'candidate-card';
        card.innerHTML = `
            <div class="candidate-summary">
                <div class="score-ring ${scoreClass}" title="Match score">${escapeHtml(score)}</div>
                <div class="candidate-name">
                    <strong>${index + 1}. ${escapeHtml(candidate.fullName || 'Unknown candidate')}</strong>
                    <span>${escapeHtml(candidate.jobTitle || candidate.email || candidate.originalFileName)}</span>
                </div>
                <div class="skill-preview">
                    ${previewSkills.slice(0, 4).map(skill => `<span class="chip match">${escapeHtml(skill)}</span>`).join('')}
                    ${previewSkills.length > 4 ? `<span class="chip">+${previewSkills.length - 4}</span>` : ''}
                </div>
                <span class="status ${statusClass}">${escapeHtml(candidate.status.replaceAll('_', ' '))}</span>
            </div>
            <details class="candidate-details">
                <summary>View scoring evidence</summary>
                <div class="evidence-grid">
                    <div class="evidence-box full">
                        <strong>AI justification</strong>
                        <p>${escapeHtml(candidate.justification || 'Candidate has not been screened yet.')}</p>
                    </div>
                    <div class="evidence-box">
                        <strong>Matched skills</strong>
                        <div class="skill-preview">
                            ${matched.length ? matched.map(skill => `<span class="chip match">${escapeHtml(skill)}</span>`).join('') : '<p>None identified</p>'}
                        </div>
                    </div>
                    <div class="evidence-box">
                        <strong>Missing skills</strong>
                        <div class="skill-preview">
                            ${missing.length ? missing.map(skill => `<span class="chip missing">${escapeHtml(skill)}</span>`).join('') : '<p>None identified</p>'}
                        </div>
                    </div>
                    <div class="evidence-box full">
                        <strong>Decision metadata</strong>
                        <p>Recommendation: ${escapeHtml(candidate.recommendation || 'Pending')} · Source: ${escapeHtml(candidate.analysisSource || 'Not analysed')}</p>
                    </div>
                    <div class="evidence-box full improvement-box">
                        <strong>How to improve this resume's ATS match</strong>
                        ${improvements.length
                            ? `<ol class="improvement-list">${improvements.map(item => `<li>${escapeHtml(item)}</li>`).join('')}</ol>`
                            : '<p>No improvement recommendations are available yet.</p>'}
                    </div>
                </div>
                <button class="delete-button" data-id="${candidate.id}">Delete candidate</button>
            </details>`;

        card.querySelector('.delete-button').addEventListener('click', () => deleteCandidate(candidate.id));
        candidateList.appendChild(card);
    });
}

async function deleteCandidate(candidateId) {
    if (!window.confirm('Delete this candidate and the stored resume?')) {
        return;
    }
    try {
        const response = await fetch(`/api/candidates/${candidateId}`, { method: 'DELETE' });
        if (!response.ok) {
            const payload = await readPayload(response);
            throw new Error(payload.message || 'Could not delete candidate.');
        }
        await loadCandidates();
    } catch (error) {
        showAlert(error.message, 'error');
    }
}

async function readPayload(response) {
    const text = await response.text();
    if (!text) return {};
    try { return JSON.parse(text); } catch { return { message: text }; }
}

function setLoading(active) {
    loadingOverlay.classList.toggle('active', active);
    loadingOverlay.setAttribute('aria-hidden', String(!active));
    screenButton.disabled = active;
}

function showAlert(message, type) {
    alertBox.textContent = message;
    alertBox.className = `alert ${type}`;
}

function clearAlert() {
    alertBox.textContent = '';
    alertBox.className = 'alert';
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

loadCandidates();
